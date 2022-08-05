package xyz.xenondevs.nova.tileentity.network

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.world.event.NovaChunkLoadedEvent
import xyz.xenondevs.nova.data.world.legacy.LegacyFileConverter
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.ObservableLock
import xyz.xenondevs.nova.util.concurrent.lockAndRun
import xyz.xenondevs.nova.util.concurrent.mapToAllFuture
import xyz.xenondevs.nova.util.concurrent.tryLockAndRun
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.filterIsInstanceValues
import xyz.xenondevs.nova.util.flatMap
import xyz.xenondevs.nova.util.pollFirst
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.thread

typealias NetworkManagerTask = (NetworkManager) -> Unit

private typealias PartialTask = () -> Unit

class NoNetworkDataException(override val message: String? = null) : Exception()

private var NETWORK_MANAGER = NetworkManagerImpl()

interface NetworkManager {
    
    val networks: List<Network>
    
    fun addEndPoint(endPoint: NetworkEndPoint, updateBridges: Boolean = true): CompletableFuture<Void>
    
    fun addBridge(bridge: NetworkBridge, updateBridges: Boolean = true): CompletableFuture<Void>
    
    fun removeEndPoint(endPoint: NetworkEndPoint, updateBridges: Boolean = true)
    
    fun removeBridge(bridge: NetworkBridge, updateBridges: Boolean = true)
    
    fun reloadNetworks()
    
    companion object : Initializable(), Listener {
        
        /**
         * Schedules loading the network in that chunk
         */
        fun queueChunkLoad(pos: ChunkPos) {
            NETWORK_MANAGER.chunkLoadQueue += pos
        }
        
        /**
         * Schedules unloading the network in that chunk
         */
        fun unloadChunk(pos: ChunkPos) {
            NETWORK_MANAGER.lock.lockAndRun {
                if (pos in NETWORK_MANAGER.chunkLoadQueue) {
                    // The chunk has not been loaded yet, remove it from the queue
                    NETWORK_MANAGER.chunkLoadQueue -= pos
                } else {
                    // The chunk has been loaded, unload it
                    NETWORK_MANAGER.unloadChunk(pos)
                }
            }
        }
        
        /**
         * Schedules the execution of this [NetworkManagerTask].
         * This task will be run asynchronously.
         */
        fun queueAsync(task: NetworkManagerTask) {
            NETWORK_MANAGER.asyncQueue += task
        }
        
        /**
         * Schedules the execution of this [NetworkManagerTask].
         * The task will be run in the main thread when the [NetworkManager] isn't busy.
         */
        fun queueSync(task: NetworkManagerTask) {
            NETWORK_MANAGER.syncQueue += task
        }
        
        /**
         * Blocks this thread until the [NetworkManager] isn't busy anymore and then runs
         * the given [NetworkManagerTask].
         */
        fun execute(task: NetworkManagerTask) {
            NETWORK_MANAGER.lock.lockAndRun {
                task.invoke(NETWORK_MANAGER)
            }
        }
        
        /**
         * Runs the given [NetworkManagerTask] if the [NetworkManager] is not busy at
         * the moment.
         */
        fun tryExecute(task: NetworkManagerTask) {
            NETWORK_MANAGER.lock.tryLockAndRun {
                task.invoke(NETWORK_MANAGER)
            }
        }
        
        override val inMainThread = true
        override val dependsOn = setOf(LegacyFileConverter)
        
        override fun init() {
            LOGGER.info("Starting network threads")
            NETWORK_MANAGER.init()
            Bukkit.getPluginManager().registerEvents(this, NOVA)
        }
        
        override fun disable() {
            LOGGER.info("Unloading networks")
            PermanentStorage.store("legacyNetworkChunks", NETWORK_MANAGER.legacyNetworkChunks)
            Bukkit.getWorlds().flatMap(World::getLoadedChunks).forEach { unloadChunk(it.pos) }
        }
        
        @EventHandler(priority = EventPriority.HIGHEST)
        private fun handleChunkLoad(event: NovaChunkLoadedEvent) {
            queueChunkLoad(event.chunkPos)
        }
        
        @EventHandler(priority = EventPriority.LOWEST)
        private fun handleChunkUnload(event: ChunkUnloadEvent) {
            unloadChunk(event.chunk.pos)
        }
        
    }
    
}

private class NetworkManagerImpl : NetworkManager {
    
    // thread-safe properties
    val lock = ObservableLock()
    val chunkLoadQueue = ConcurrentHashMap.newKeySet<ChunkPos>()
    val syncQueue = ConcurrentLinkedQueue<NetworkManagerTask>()
    val asyncQueue = ConcurrentLinkedQueue<NetworkManagerTask>()
    val partialTaskQueue = ConcurrentLinkedQueue<PartialTask>()
    
    // non-thread-safe properties
    override val networks = ArrayList<Network>()
    val networksById = HashMap<UUID, Network>()
    val nodesById = HashMap<UUID, NetworkNode>()
    
    val legacyNetworkChunks: HashSet<ChunkPos> by lazy { PermanentStorage.retrieve("legacyNetworkChunks") { hashSetOf() } }
    
    fun init() {
        runTaskTimer(0, 1) {
            // If the NetworkManager is busy at the moment, sync tasks will not be executed
            // in order to not block the main thread.
            // Sync tasks are delayed, network ticks are skipped.
            lock.tryLockAndRun {
                networks.forEach { network ->
                    if (network is ItemNetwork) {
                        if (serverTick % 20 == 0) network.handleTick()
                    } else network.handleTick()
                }
                
                while (syncQueue.isNotEmpty()) {
                    val task = syncQueue.poll()
                    task.invoke(this)
                }
            }
        }
        
        thread(isDaemon = true, name = "Nova NetworkManager") {
            while (NOVA.isEnabled) {
                try {
                    while (partialTaskQueue.isNotEmpty() || asyncQueue.isNotEmpty() || chunkLoadQueue.isNotEmpty()) {
                        // partial tasks have priority
                        while (partialTaskQueue.isNotEmpty()) {
                            val task = partialTaskQueue.poll()
                            lock.lockAndRun(task)
                        }
                        
                        // only take tasks from the async queue when there are no partial tasks
                        while (asyncQueue.isNotEmpty() && partialTaskQueue.isEmpty()) {
                            val task = asyncQueue.poll()
                            lock.lockAndRun { task.invoke(this) }
                        }
                        
                        // only load chunks if the other queues are empty
                        while (chunkLoadQueue.isNotEmpty() && asyncQueue.isEmpty() && partialTaskQueue.isEmpty()) {
                            val pos = chunkLoadQueue.pollFirst() ?: break
                            lock.lockAndRun { loadChunk(pos) }
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.log(Level.SEVERE, "An exception occurred in a NetworkManager task", e)
                }
                
                // All tasks are done for this tick
                Thread.sleep(50)
            }
        }
    }
    
    private fun getNodesInChunk(pos: ChunkPos): List<NetworkNode> {
        val nodes = ArrayList<NetworkNode>()
        nodes += TileEntityManager.getTileEntitiesInChunk(pos).filterIsInstance<NetworkNode>()
        nodes += VanillaTileEntityManager.getTileEntitiesInChunk(pos).filterIsInstance<NetworkNode>()
        
        return nodes
    }
    
    private fun getNetwork(type: NetworkType, uuid: UUID = UUID.randomUUID(), local: Boolean = false): Network {
        return networksById.getOrPut(uuid) { type.networkConstructor(uuid, local).also(networks::add) }
    }
    
    private fun removeNetwork(network: Network) {
        networks -= network
        networksById -= network.uuid
    }
    
    private fun removeNetworks(networks: Iterable<Network>) {
        networks.forEach(this::removeNetwork)
    }
    
    override fun reloadNetworks() {
        val chunks = Bukkit.getWorlds().flatMap(World::getLoadedChunks).map(Chunk::pos)
        chunks.forEach { unloadChunk(it, true) }
        networks.clear()
        chunks.forEach { loadChunk(it, true) }
    }
    
    private fun loadChunk(pos: ChunkPos, loadNodesIndividually: Boolean = false) {
        val nodes = getNodesInChunk(pos)
        
        if (loadNodesIndividually || pos in legacyNetworkChunks) {
            loadNodesIndividually(nodes)
            legacyNetworkChunks -= pos
            return
        }
        
        try {
            // A map of all networks and the network nodes that need to be added to them
            // after all nodes in this chunk have been loaded.
            val networks = HashMap<Network, MutableList<Pair<BlockFace?, NetworkNode>>>()
            
            nodes.forEach { node ->
                val uuid = node.uuid
                nodesById[uuid] = node
                
                if (node is NetworkBridge) {
                    loadNetworkBridge(node).forEach { network ->
                        networks.getOrPut(network) { ArrayList() } += null to node
                    }
                } else if (node is NetworkEndPoint) {
                    loadNetworkEndPoint(node).forEach { (face, list) ->
                        list.forEach { network -> networks.getOrPut(network) { ArrayList() } += face to node }
                    }
                }
            }
            
            // Add all the network nodes to their networks
            networks.forEach { (network, nodes) -> network.addAll(nodes) }
        } catch (e: NoNetworkDataException) {
            loadNodesIndividually(nodes)
        }
    }
    
    private fun loadNodesIndividually(nodes: List<NetworkNode>) {
        nodes.forEach {
            if (it is NetworkBridge) addBridge(it)
            else addEndPoint(it as NetworkEndPoint)
        }
    }
    
    /**
     * Loads the [NetworkNode.connectedNodes] map of the [NetworkNode]
     *
     * @throws NoNetworkDataException If no data for the connectedNodes map was found
     */
    private fun loadNetworkNode(node: NetworkNode) {
        val serializedConnectedNodes = node.retrieveSerializedConnectedNodes() ?: throw NoNetworkDataException()
        serializedConnectedNodes.forEach { (networkType, faceMap) ->
            faceMap.forEach faces@{ (face, connectedUUID) ->
                // The connected node might not be loaded yet
                val connected = nodesById[connectedUUID] ?: return@faces
                
                // Connect that node to this node at this face
                node.setConnectedNode(networkType, face, connected)
                
                // Also connect this node to the connectedNode, as the current node was not yet loaded 
                // when the connectedNode ran through this code
                connected.setConnectedNode(networkType, face.oppositeFace, node)
            }
        }
    }
    
    /**
     * Loads the [NetworkEndPoint.networks] map of the [NetworkEndPoint]
     *
     * @return A map of the attached [Networks][Network] on each [BlockFace], to which the [NetworkEndPoint]
     * needs to be added to
     * @throws NoNetworkDataException If no data for the networks map was found
     */
    private fun loadNetworkEndPoint(endPoint: NetworkEndPoint): Map<BlockFace, List<Network>> {
        loadNetworkNode(endPoint)
        val serializedNetworks = endPoint.retrieveSerializedNetworks() ?: throw NoNetworkDataException()
        
        val networks = emptyEnumMap<BlockFace, MutableList<Network>>()
        
        serializedNetworks.forEach { (networkType, faceMap) ->
            faceMap.forEach faces@{ (face, networkUUID) ->
                // Retrieve the network with that UUID or create a new one
                val network = getNetwork(networkType, networkUUID)
                
                // Set the network in the networks map
                endPoint.setNetwork(networkType, face, network)
                
                // Add the network to the list of networks
                networks.getOrPut(face) { ArrayList() } += network
            }
        }
        
        return networks
    }
    
    /**
     * Loads the [NetworkBridge.networks] map of [NetworkBridge]
     *
     * @return A list of [Networks][Network] the [NetworkBridge] needs to be added to
     * @throws NoNetworkDataException If no data for the networks map was found
     */
    private fun loadNetworkBridge(bridge: NetworkBridge): List<Network> {
        loadNetworkNode(bridge)
        val serializedNetworks = bridge.retrieveSerializedNetworks() ?: throw NoNetworkDataException()
        
        // the networks that the bridge needs to be added to
        val networks = serializedNetworks.map { (networkType, networkUUID) ->
            // Retrieve the network with that UUID or create a new one
            val network = getNetwork(networkType, networkUUID)
            
            // Set the network in the networks map
            bridge.setNetwork(networkType, network)
            
            return@map network
        }
        
        return networks
    }
    
    fun unloadChunk(pos: ChunkPos, resetInternalData: Boolean = false) {
        val nodes = getNodesInChunk(pos)
        val networks = HashMap<Network, MutableList<NetworkNode>>()
        
        nodes.forEach { node ->
            when (node) {
                is NetworkBridge -> unloadNetworkBridge(node, resetInternalData)
                is NetworkEndPoint -> unloadNetworkEndPoint(node, resetInternalData)
            }.forEach { network -> networks.getOrPut(network) { ArrayList() } += node }
        }
        
        // Remove the nodes from their networks
        networks.forEach { (network, nodes) -> network.removeAll(nodes) }
    }
    
    /**
     * Removes this node from the [NetworkNode.connectedNodes] map of attached nodes and clears
     * its own [NetworkNode.connectedNodes] map.
     */
    private fun resetNetworkNode(node: NetworkNode) {
        // remove this node from the connectedNodes map of connected nodes
        node.connectedNodes.forEach { (networkType, faceMap) ->
            faceMap.forEach { (face, node) ->
                node.removeConnectedNode(networkType, face.oppositeFace)
            }
        }
        
        // clear the connectedNodes map
        node.connectedNodes.clear()
    }
    
    /**
     * Returns a list of [Networks][Network] the [endPoint] needs to be removed
     *
     * @param resetInternalData If the [NetworkEndPoint] should be removed from the [NetworkNode.connectedNodes] map of
     * connected nodes and both [NetworkNode.connectedNodes] and [NetworkEndPoint.networks] should be cleared
     *
     * @return A list of [Networks][Network] the [NetworkEndPoint] needs to be removed from
     */
    private fun unloadNetworkEndPoint(endPoint: NetworkEndPoint, resetInternalData: Boolean = false): List<Network> {
        // a list of networks this endpoint is a part of and needs to be removed from
        val networks = endPoint.networks.flatMapTo(ArrayList()) { it.value.values }
        
        if (resetInternalData) {
            resetNetworkNode(endPoint)
            endPoint.networks.clear()
        }
        
        return networks
    }
    
    /**
     * Returns a list of [Networks][Network] the [bridge] needs to be removed
     *
     * @param resetInternalData If the [NetworkBridge] should be removed from the [NetworkNode.connectedNodes] map of
     * connected nodes and both [NetworkNode.connectedNodes] and [NetworkBridge.networks] should be cleared
     *
     * @return A list of [Networks][Network] the [NetworkEndPoint] needs to be removed from
     */
    private fun unloadNetworkBridge(bridge: NetworkBridge, resetInternalData: Boolean = false): List<Network> {
        // a list of networks this bridge is a part of and needs to be removed from
        val networks = bridge.networks.values.toList()
        
        if (resetInternalData) {
            resetNetworkNode(bridge)
            bridge.networks.clear()
        }
        
        return networks
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, updateBridges: Boolean): CompletableFuture<Void> {
        // add endPoint to nodesById
        nodesById[endPoint.uuid] = endPoint
        
        return NetworkTypeRegistry.types.mapToAllFuture networks@{ networkType ->
            val allowedFaces = endPoint.allowedFaces[networkType]
            if (allowedFaces != null) { // does the endpoint want to have any connections?
                // loop over all bridges nearby to possibly connect to
                return@networks endPoint.getNearbyNodes().mapToAllFuture endPoints@{ (face, neighborNode) ->
                    // does the endpoint want a connection at that face?
                    if (!allowedFaces.contains(face)) return@endPoints null
                    // do not allow networks between two vanilla tile entities
                    if (endPoint is VanillaTileEntity && neighborNode is VanillaTileEntity) return@endPoints null
                    
                    return@endPoints hasAccessPermission(endPoint, neighborNode).thenRunPartialTask(endPoint) {
                        connectEndPoint(endPoint, neighborNode, networkType, face, updateBridges)
                    }
                }
            } else return@networks null
        }
    }
    
    private fun connectEndPoint(endPoint: NetworkEndPoint, neighborNode: NetworkNode, networkType: NetworkType, face: BlockFace, updateBridges: Boolean) {
        val oppositeFace = face.oppositeFace
        
        if (neighborNode is NetworkBridge) {
            if (neighborNode.networks[networkType] != null // is the bridge in a network of this type
                && neighborNode.bridgeFaces.contains(oppositeFace)) { // does the bridge want a connection at that face
                
                // add to network
                val network = neighborNode.networks[networkType]!!
                endPoint.setNetwork(networkType, face, network)
                network.addEndPoint(endPoint, face)
                
                // tell the bridge that we connected to it
                neighborNode.setConnectedNode(networkType, oppositeFace, endPoint)
                // remember that we connected to it
                endPoint.setConnectedNode(networkType, face, neighborNode)
                
                if (updateBridges) neighborNode.handleNetworkUpdate()
            }
        } else if (
            neighborNode is NetworkEndPoint
            && neighborNode.allowedFaces[networkType]?.contains(oppositeFace) == true // does the endpoint want a connection of this type at that face
            && neighborNode.getConnectedNode(networkType, oppositeFace) == null // does not already connect to something there
        ) {
            // create a new "local" network
            val network = getNetwork(networkType, local = true)
            network.addEndPoint(endPoint, face)
            network.addEndPoint(neighborNode, face.oppositeFace)
            
            // would this network make sense? (i.e. no networks of only consumers or only providers)
            if (network.isValid()) {
                // tell the neighbor that is now connected to this endPoint over the network at that face
                neighborNode.setConnectedNode(networkType, face.oppositeFace, endPoint)
                neighborNode.setNetwork(networkType, oppositeFace, network)
                // remember that we're now connected to that node over the network at that face
                endPoint.setConnectedNode(networkType, face, neighborNode)
                endPoint.setNetwork(networkType, face, network)
            } else removeNetwork(network)
        }
    }
    
    override fun addBridge(bridge: NetworkBridge, updateBridges: Boolean): CompletableFuture<Void> {
        // add bridge to nodesById
        nodesById[bridge.uuid] = bridge
        
        val nearbyNodes: Map<BlockFace, NetworkNode> = bridge.getNearbyNodes()
        val nearbyBridges: Map<BlockFace, NetworkBridge> = nearbyNodes.filterIsInstanceValues()
        val nearbyEndPoints: Map<BlockFace, NetworkEndPoint> = nearbyNodes.filterIsInstanceValues()
        
        return bridge.supportedNetworkTypes.mapToAllFuture { networkType ->
            val bridgeAccessFutures = nearbyBridges.entries.asSequence()
                .filter { (face, otherBridge) -> bridge.canConnect(otherBridge, networkType, face) }
                .associateWith { hasAccessPermission(bridge, it.value) }
            
            val endPointAccessFutures = nearbyEndPoints.entries.asSequence()
                .filter { (face, otherEndPoint) -> bridge.canConnect(otherEndPoint, networkType, face) }
                .associateWith { hasAccessPermission(bridge, it.value) }
            
            return@mapToAllFuture CompletableFuture.allOf(*(bridgeAccessFutures.values + endPointAccessFutures.values).toTypedArray())
                .thenRunPartialTask(bridge) {
                    val network = connectBridgeToBridges(bridge, networkType, bridgeAccessFutures)
                    connectBridgeToEndPoints(bridge, networkType, network, endPointAccessFutures)
                }
        }.thenRunPartialTask(bridge) {
            if (updateBridges) {
                // update nearby bridges
                bridge.updateNearbyBridges()
            }
            
            // update itself
            bridge.handleNetworkUpdate()
        }
    }
    
    private fun connectBridgeToBridges(
        bridge: NetworkBridge,
        networkType: NetworkType,
        accessPermissionFutures: Map<Map.Entry<BlockFace, NetworkBridge>, CompletableFuture<Boolean>>
    ): Network {
        val previousNetworks = HashSet<Network>()
        
        accessPermissionFutures.forEach { (entry, future) ->
            // has access permission?
            if (future.get()) {
                val (face, otherBridge) = entry
                
                // bridges won't have a network if they haven't been fully initialized yet
                if (otherBridge.networks.containsKey(networkType)) {
                    // a possible network to connect to
                    previousNetworks += otherBridge.networks[networkType]!!
                }
                
                // tell that bridge we connected to it
                otherBridge.setConnectedNode(networkType, face.oppositeFace, bridge)
                // remember that we connected to it
                bridge.setConnectedNode(networkType, face, otherBridge)
            }
        }
        
        // depending on how many possible networks there are, perform the required action
        val network = when {
            // Merge networks
            previousNetworks.size > 1 -> {
                // remove old networks
                removeNetworks(previousNetworks)
                // create new network
                val newNetwork = getNetwork(networkType)
                
                // move nodes from all previous networks to new network
                previousNetworks.forEach { network ->
                    network.nodes.forEach { node -> node.move(network, newNetwork) }
                    newNetwork.addAll(network)
                }
                
                newNetwork
            }
            
            // Connect to network
            previousNetworks.size == 1 -> previousNetworks.first()
            
            // Make a completely new network
            else -> getNetwork(networkType)
        }
        
        // Add the Bridge to the network
        bridge.setNetwork(networkType, network)
        network.addBridge(bridge)
        
        return network
    }
    
    private fun connectBridgeToEndPoints(
        bridge: NetworkBridge,
        networkType: NetworkType,
        network: Network,
        accessPermissionFutures: Map<Map.Entry<BlockFace, NetworkEndPoint>, CompletableFuture<Boolean>>
    ) {
        accessPermissionFutures.forEach { (entry, future) ->
            // has access permission?
            if (future.get()) {
                val (face, endPoint) = entry
                val oppositeFace = face.oppositeFace
                
                // add to network
                endPoint.setNetwork(networkType, oppositeFace, network)
                network.addEndPoint(endPoint, oppositeFace)
                
                // tell the endpoint that we connected to it
                endPoint.setConnectedNode(networkType, oppositeFace, bridge)
                // remember that we connected to that endpoint
                bridge.setConnectedNode(networkType, face, endPoint)
            }
        }
    }
    
    override fun removeEndPoint(endPoint: NetworkEndPoint, updateBridges: Boolean) {
        // remove endPoint from nodesById
        nodesById -= endPoint.uuid
        
        // remove this node from the connectedNodes map of connected nodes
        endPoint.connectedNodes.forEach { (networkType, faceMap) ->
            faceMap.forEach { (face, node) ->
                node.removeConnectedNode(networkType, face.oppositeFace)
            }
        }
        
        endPoint.networks.forEach { (_, networkMap) ->
            networkMap.forEach { (_, network) ->
                network.removeNode(endPoint)
                
                // remove the network from networks if it isn't valid
                if (!network.isValid()) removeNetwork(network)
            }
        }
        
        endPoint.connectedNodes.clear()
        endPoint.networks.clear()
        
        if (updateBridges) endPoint.updateNearbyBridges()
    }
    
    override fun removeBridge(bridge: NetworkBridge, updateBridges: Boolean) {
        // remove bridge from nodesById
        nodesById -= bridge.uuid
        
        bridge.networks.forEach { (networkType, currentNetwork) ->
            // counter to keep track of how many other bridges are directly connected
            var connectedBridgeCount = 0
            // disconnect this bridge from neighboring nodes
            bridge.connectedNodes[networkType]?.forEach { (face, node) ->
                val oppositeFace = face.oppositeFace
                if (node is NetworkEndPoint) {
                    // there is no longer a network connection at this block face
                    node.removeNetwork(networkType, oppositeFace)
                    node.removeConnectedNode(networkType, oppositeFace)
                    
                    // remove from network in its current ConnectionType
                    currentNetwork.removeNode(node)
                    if (node.getFaceMap(networkType).filter { (_, network) -> network == currentNetwork }.isNotEmpty()) {
                        // there are still connections to that EndPoint, but it may not have full functionality anymore
                        node.getFaceMap(networkType).forEach { (face, network) ->
                            if (network == currentNetwork) currentNetwork.addEndPoint(node, face)
                        }
                    }
                } else if (node is NetworkBridge) {
                    // remove this bridge the connectedNodes map of the bridge this bridge is connected to
                    node.removeConnectedNode(networkType, oppositeFace)
                    // count connected bridges
                    connectedBridgeCount++
                }
            }
            
            // if the bridge was connected to multiple other bridges, recalculate networks
            if (connectedBridgeCount > 1) {
                val recalculatedNetworkedNodes = recalculateNetworks(bridge, networkType)
                if (recalculatedNetworkedNodes.size > 1) {
                    // remove the previous network
                    removeNetwork(currentNetwork)
                    
                    // create new networks
                    recalculatedNetworkedNodes.forEach { nodes ->
                        val network = getNetwork(networkType)
                        
                        nodes.forEach { (face, node) ->
                            if (node is NetworkBridge) {
                                node.setNetwork(networkType, network)
                            } else if (node is NetworkEndPoint) {
                                node.setNetwork(networkType, face, network)
                            }
                        }
                        
                        network.addAll(nodes)
                    }
                    
                } else {
                    // keep the current network, just remove the bridge from it
                    currentNetwork.removeNode(bridge)
                }
            } else {
                // just remove the bridge from the current network
                currentNetwork.removeNode(bridge)
                
                // remove the network if it isn't valid
                if (!currentNetwork.isValid()) removeNetwork(currentNetwork)
            }
        }
        
        bridge.connectedNodes.clear()
        bridge.networks.clear()
        
        // update nearby bridges
        if (updateBridges) bridge.updateNearbyBridges()
    }
    
    /**
     * Recalculates networks for the case that [bridge] was destroyed. This method assumes that [bridge] has
     * been removed from the [NetworkNode.connectedNodes] map of attached [NetworkNodes][NetworkNode].
     *
     * @return A list of sets that contains a [Pair]<[BlockFace], [NetworkNode]>, whereas each set is its own network
     * and the [BlockFace] corresponds to the side the [NetworkNode] is connected with (from the perspective of the
     * given [NetworkNode]). It is guaranteed that the returned list does not contain duplicates.
     */
    private fun recalculateNetworks(bridge: NetworkBridge, networkType: NetworkType): List<Set<Pair<BlockFace, NetworkNode>>> {
        val networks = ArrayList<Set<Pair<BlockFace, NetworkNode>>>()
        
        val previouslyExploredBridges = HashSet<NetworkBridge>()
        
        sideIteration@ for ((face, startNode) in bridge.connectedNodes[networkType]!!) {
            // a set of nodes that was already explored in this side iteration
            val exploredNodes = HashSet<NetworkNode>()
            // a set of bridges that was explored in this side iteration
            // after this side iteration is done, it is added to previouslyExploredBridges
            val exploredBridges = HashSet<NetworkBridge>()
            // the content of the network that is being explored in this side iteration
            val connectedNodeFaces = HashSet<Pair<BlockFace, NetworkNode>>()
            
            var unexploredNodes = ArrayList<Pair<BlockFace, NetworkNode>>(1)
            unexploredNodes.add(face to startNode)
            
            // loop until all nodes are explored
            while (unexploredNodes.size != 0) {
                val newUnexploredNodes = ArrayList<Pair<BlockFace, NetworkNode>>(6)
                
                for ((approachingFace, nodeToExplore) in unexploredNodes) {
                    
                    if (nodeToExplore is NetworkBridge) {
                        // loop over all the nodes that that bridge is connected to
                        for (connectedEntry in nodeToExplore.connectedNodes[networkType]!!) {
                            val node = connectedEntry.value
                            
                            // are we back at the beginning? prevent infinite loops
                            if (node == bridge) continue
                            
                            // if this bridge was already explored when we started from a different side,
                            // this run will just be a duplicate of a previous side iteration and should be stopped here
                            if (previouslyExploredBridges.contains(node)) break@sideIteration
                            
                            // check if the node was already explored in this run
                            if (exploredNodes.contains(node)) {
                                // the connection from a different side might still be important
                                connectedNodeFaces += approachingFace.oppositeFace to nodeToExplore
                            } else {
                                // that node should also be explored
                                newUnexploredNodes += connectedEntry.toPair()
                                exploredNodes += node
                            }
                        }
                        
                        exploredBridges += nodeToExplore
                    }
                    
                    // node is now explored, add to network content
                    connectedNodeFaces += approachingFace.oppositeFace to nodeToExplore
                }
                
                unexploredNodes = newUnexploredNodes
            }
            
            // prevent networks that are only made up of one end point and nothing else
            if (connectedNodeFaces.size > 1 || connectedNodeFaces.first().second is NetworkBridge) {
                previouslyExploredBridges += exploredBridges
                networks += connectedNodeFaces
            }
        }
        
        return networks
    }
    
    private fun hasAccessPermission(source: NetworkNode, target: NetworkNode): CompletableFuture<Boolean> {
        val futures = ArrayList<CompletableFuture<Boolean>>()
        if (source is TileEntity) futures += ProtectionManager.canUseBlock(source, null, target.location)
        if (target is TileEntity) futures += ProtectionManager.canUseBlock(target, null, source.location)
        return CombinedBooleanFuture(futures)
    }
    
    private fun CompletableFuture<*>.thenRunPartialTask(node: NetworkNode, task: () -> Unit): CompletableFuture<Void> {
        val taskExecutionFuture = CompletableFuture<Void>()
        
        // the task will not be run and the future will never be complete when the node has already been removed
        if (node.uuid !in nodesById)
            return taskExecutionFuture
        
        thenRun {
            partialTaskQueue += {
                task()
                taskExecutionFuture.complete(null)
            }
        }
        
        return taskExecutionFuture
    }
    
}