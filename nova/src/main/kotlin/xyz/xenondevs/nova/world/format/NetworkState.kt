@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.world.format

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.bukkit.World
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.collections.associateWithNotNullTo
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.GhostNetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import xyz.xenondevs.nova.world.format.chunk.NetworkNodeData
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.isSuperclassOf

/**
 * Contains or links to all network-related data for a specific [World].
 *
 * Unless otherwise specified, all functions in this class are only to be
 * called from the network configurator context.
 */
class NetworkState internal constructor(
    private val world: World,
    internal val storage: WorldDataStorage
) {
    
    private val networksById = ConcurrentHashMap<UUID, Deferred<ProtoNetwork<*>?>>()
    private val nodesByPos = HashMap<BlockPos, NetworkNode>()
    
    private val pendingRemovalNetworks = HashSet<UUID>()
    private val pendingUnloadNetworks = ConcurrentHashMap<UUID, ProtoNetwork<*>>()
    
    /**
     * Mutex for all data governed by this [NetworkState].
     */
    internal val mutex = Mutex()
    
    /**
     * A sequence of all loaded [ProtoNetworks][ProtoNetwork].
     *
     * @throws IllegalStateException If a network is currently being loaded
     */
    val networks: Sequence<ProtoNetwork<*>>
        get() = networksById.values.asSequence()
            .map { it.getCompleted() }
            .filterNotNull()
    
    /**
     * Adds [node] to the network state.
     */
    operator fun plusAssign(node: NetworkNode) {
        nodesByPos[node.pos] = node
    }
    
    /**
     * Removes [node] from the network state.
     */
    operator fun minusAssign(node: NetworkNode) {
        nodesByPos -= node.pos
    }
    
    /**
     * Adds [network] to the network state.
     */
    operator fun plusAssign(network: ProtoNetwork<*>) {
        networksById[network.uuid] = CompletableDeferred(network)
    }
    
    /**
     * Adds all [networks] to the network state.
     */
    operator fun plusAssign(networks: Iterable<ProtoNetwork<*>>) {
        for (network in networks) {
            this += network
        }
    }
    
    /**
     * Removes the [network] from the network state and schedules the deletion
     * of its data from disk.
     */
    fun deleteNetwork(network: ProtoNetwork<*>) {
        networksById -= network.uuid
        pendingRemovalNetworks += network.uuid
    }
    
    /**
     * Unloads the [network] from the network state, but does not delete
     * any data associated with it.
     */
    fun unloadNetwork(network: ProtoNetwork<*>) {
        networksById -= network.uuid
        pendingUnloadNetworks[network.uuid] = network
    }
    
    /**
     * Checks whether a network with the same UUID as [network] is present in the network state.
     */
    operator fun contains(network: ProtoNetwork<*>) =
        networksById.containsKey(network.uuid)
    
    /**
     * Checks whether a node with the same position as [node] is present in the network state.
     */
    operator fun contains(node: NetworkNode) =
        node.pos in nodesByPos
    
    /**
     * Creates a new [ProtoNetwork] with the given [type] and [networkId].
     */
    fun <T : Network<T>> createNetwork(type: NetworkType<T>, networkId: UUID = UUID.randomUUID()): ProtoNetwork<T> {
        val network = ProtoNetwork(this, type, networkId)
        networksById[networkId] = CompletableDeferred(network)
        return network
    }
    
    /**
     * Resolves a network by its [networkId], loading it from disk if necessary.
     */
    suspend fun resolveNetwork(networkId: UUID): ProtoNetwork<*> = coroutineScope {
        return@coroutineScope networksById.computeIfAbsent(networkId) {
            // The network might be unloaded, but not yet written to disk
            val pendingNetwork = pendingUnloadNetworks.remove(networkId)
            if (pendingNetwork != null)
                return@computeIfAbsent CompletableDeferred(pendingNetwork)
            
            // The network needs to be read from disk
            async(Dispatchers.IO) { loadNetwork(networkId) }
        }.await() ?: throw IllegalArgumentException("Network with id $networkId not found")
    }
    
    /**
     * Attempts to load a [ProtoNetwork] from disk, returning `null` if
     * no network file exists for the given [networkId].
     *
     * May suspend to await network region load.
     */
    private suspend fun loadNetwork(networkId: UUID): ProtoNetwork<*>? {
        val file = File(storage.networkFolder, "$networkId.nvnt")
        if (!file.exists())
            return null
        
        val network = file.inputStream().use { inp ->
            val reader = ByteReader.fromStream(inp)
            ProtoNetwork.read(networkId, world, this, reader)
        }
        
        return network
    }
    
    /**
     * Resolves a [NetworkNode] by its [pos].
     *
     * @throws IllegalStateException If there is no data for a node at [pos].
     */
    suspend fun resolveNode(pos: BlockPos): NetworkNode {
        val node = nodesByPos[pos]
        if (node != null)
            return node
        
        return GhostNetworkNode.fromData(pos, this.getNodeData(pos))
    }
    
    /**
     * Finds all nearby [NetworkNodes][NetworkNode] of [pos] using the given [faces].
     */
    fun getNearbyNodes(pos: BlockPos, faces: Set<BlockFace>): Map<BlockFace, NetworkNode> =
        faces.associateWithNotNullTo(enumMap<BlockFace, NetworkNode>()) { face -> nodesByPos[pos.advance(face, 1)] }
    
    // ---- Data ----
    
    /**
     * Gets all network node data for the given [pos].
     */
    suspend fun getNodeData(pos: ChunkPos): Map<BlockPos, NetworkNodeData> =
        storage.getOrLoadNetworkChunk(pos).getData()
    
    /**
     * Gets the [NetworkNodeData] for [pos], potentially loading the corresponding
     * network region if necessary and throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for a node at [pos].
     */
    suspend fun getNodeData(pos: BlockPos): NetworkNodeData =
        storage.getOrLoadNetworkChunk(pos.chunkPos).getData(pos)
            ?: throw IllegalStateException("No data for node at $pos")
    
    /**
     * Gets the [NetworkNodeData] for [node], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun getNodeData(node: NetworkNode): NetworkNodeData =
        this.getNodeData(node.pos)
    
    /**
     * Gets the [NetworkBridgeData] for [bridge], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getBridgeData(bridge: NetworkBridge): NetworkBridgeData =
        storage.getOrLoadNetworkChunk(bridge.pos.chunkPos).getBridgeData(bridge.pos)
            ?: throw IllegalStateException("No data for bridge at ${bridge.pos}")
    
    /**
     * Sets [data] at [pos].
     */
    suspend fun setBridgeData(pos: BlockPos, data: NetworkBridgeData) {
        storage.getOrLoadNetworkChunk(pos.chunkPos).setBridgeData(pos, data)
    }
    
    /**
     * Gets the [NetworkEndPointData] for [endPoint], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     */
    suspend fun getEndPointData(endPoint: NetworkEndPoint): NetworkEndPointData =
        storage.getOrLoadNetworkChunk(endPoint.pos.chunkPos).getEndPointData(endPoint.pos)
            ?: throw IllegalStateException("No data for endpoint at ${endPoint.pos}")
    
    /**
     * Sets [data] at [pos].
     */
    suspend fun setEndPointData(pos: BlockPos, data: NetworkEndPointData) {
        storage.getOrLoadNetworkChunk(pos.chunkPos).setEndPointData(pos, data)
    }
    
    /**
     * Removes the data for [node].
     *
     * @see getNodeData
     * @see getEndPointData
     * @see getBridgeData
     */
    suspend fun removeNodeData(node: NetworkNode) {
        storage.getOrLoadNetworkChunk(node.pos.chunkPos).setData(node.pos, null)
    }
    
    /**
     * Remembers a connection of [node] at [face] using [networkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun setConnection(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace) {
        val data = getNodeData(node)
        data.connections.getOrPut(networkType, ::enumSet) += face
    }
    
    /**
     * Forgets a connection of [node] at [face] using [networkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun removeConnection(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace) {
        val data = getNodeData(node)
        data.connections[networkType]?.remove(face)
    }
    
    /**
     * Creates a table that describes using which [NetworkType] and [BlockFace]
     * which [NetworkNodes][NetworkNode] are connected to [node].
     *
     * @throws IllegalStateException If there is no data for [node].
     *
     * @see setConnection
     * @see removeConnection
     */
    suspend fun getConnectedNodes(node: NetworkNode): Table<NetworkType<*>, BlockFace, NetworkNode> {
        val table = HashBasedTable.create<NetworkType<*>, BlockFace, NetworkNode>()
        forEachConnectedNode(node, table::put)
        return table
    }
    
    /**
     * Gets the [NetworkNode] connected to [node] at [face], or `null` if there is no connection.
     *
     * @throws IllegalStateException If there is no data for [node].
     *
     * @see setConnection
     * @see removeConnection
     */
    suspend fun getConnectedNode(node: NetworkNode, face: BlockFace): NetworkNode? {
        if (getNodeData(node).connections.none { (_, faces) -> face in faces })
            return null
        
        return resolveNode(node.pos.advance(face))
    }
    
    /**
     * Gets the [NetworkNode] connected to [node] at [face] using [networkType], or `null` if there is no connection.
     *
     * @throws IllegalStateException If there is no data for [node].
     *
     * @see setConnection
     * @see removeConnection
     */
    suspend fun getConnectedNode(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace): NetworkNode? {
        if (getNodeData(node).connections[networkType]?.contains(face) != true)
            return null
        
        return resolveNode(node.pos.advance(face))
    }
    
    /**
     * Checks whether [node] has a connection at [face] using [networkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     *
     * @see setConnection
     * @see removeConnection
     */
    suspend fun hasConnection(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace): Boolean =
        getNodeData(node).connections[networkType]?.contains(face) == true
    
    /**
     * Checks whether [node] has a connection at [face].
     *
     * @throws IllegalStateException If there is no data for [node].
     *
     * @see setConnection
     * @see removeConnection
     */
    suspend fun hasConnection(node: NetworkNode, face: BlockFace): Boolean =
        getNodeData(node).connections.any { (_, faces) -> face in faces }
    
    /**
     * Iterates over all [NetworkNodes][NetworkNode] connected to [node], calling [action] for each connection.
     * The action parameters describe using which [NetworkType] and through wich [BlockFace] the [NetworkNode] is connected.
     * Some [NetworkNodes][NetworkNode] may be connected through multiple [NetworkTypes][NetworkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend inline fun forEachConnectedNode(node: NetworkNode, action: (NetworkType<*>, BlockFace, NetworkNode) -> Unit) {
        val connections = getNodeData(node).connections
        for ((networkType, faces) in connections) {
            for (face in faces) {
                val connectedNode = resolveNode(node.pos.advance(face))
                action(networkType, face, connectedNode)
            }
        }
    }
    
    /**
     * Iterates over all [NetworkNodes][NetworkNode] connected to [node] using [networkType], calling [action] for each connection.
     * The action parameters describe through wich [BlockFace] the [NetworkNode] is connected.
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend inline fun forEachConnectedNode(node: NetworkNode, networkType: NetworkType<*>, action: (BlockFace, NetworkNode) -> Unit) {
        val faces = getNodeData(node).connections[networkType] ?: return
        for (face in faces) {
            val connectedNode = resolveNode(node.pos.advance(face))
            action(face, connectedNode)
        }
    }
    
    /**
     * Remembers the connection of [bridge] to [network] under [ProtoNetwork.type].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun setNetwork(bridge: NetworkBridge, network: ProtoNetwork<*>) {
        getBridgeData(bridge).networks[network.type] = network.uuid
    }
    
    /**
     * Remembers the connection of [endPoint] to [network] at [face].
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     */
    suspend fun setNetwork(endPoint: NetworkEndPoint, face: BlockFace, network: ProtoNetwork<*>) {
        val data = getEndPointData(endPoint)
        data.networks[network.type, face] = network.uuid
    }
    
    /**
     * Remembers the connection of [endPoint] to [network] at all [faces].
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     */
    suspend fun setNetwork(endPoint: NetworkEndPoint, faces: Iterable<BlockFace>, network: ProtoNetwork<*>) {
        val data = getEndPointData(endPoint)
        val type = network.type
        val uuid = network.uuid
        for (face in faces) {
            data.networks[type, face] = uuid
        }
    }
    
    /**
     * Forgets the connection of [bridge] to the network of [networkType].
     */
    suspend fun removeNetwork(bridge: NetworkBridge, networkType: NetworkType<*>) {
        getBridgeData(bridge).networks -= networkType
    }
    
    /**
     * Forgets the connection of [endPoint] to the network of [networkType] at [face].
     */
    suspend fun removeNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<*>, face: BlockFace) {
        getEndPointData(endPoint).networks.remove(networkType, face)
    }
    
    /**
     * Forgets the connection of [endPoint] to the network of [networkType] at all [faces].
     */
    suspend fun removeNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<*>, faces: Iterable<BlockFace>) {
        val data = getEndPointData(endPoint)
        for (face in faces) {
            data.networks.remove(networkType, face)
        }
    }
    
    /**
     * Gets the network of [endPoint] at [face], or `null` if there is no connection.
     */
    suspend fun <T : Network<T>> getNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<T>, face: BlockFace): ProtoNetwork<T>? =
        getEndPointData(endPoint).networks[networkType, face]?.let { resolveNetwork(it) } as ProtoNetwork<T>?
    
    /**
     * Gets the network map of [bridge].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getNetworks(bridge: NetworkBridge): MutableMap<NetworkType<*>, UUID> =
        getBridgeData(bridge).networks
    
    /**
     * Gets the networks table of [endPoint].
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     */
    suspend fun getNetworks(endPoint: NetworkEndPoint): Table<NetworkType<*>, BlockFace, UUID> =
        getEndPointData(endPoint).networks
    
    /**
     * Gets the network of [bridge] of [networkType], or `null` if there is no connection.
     *
     * @throws IllegalStateException If there is no data for [bridge].
     * @throws IllegalStateException If the referenced network is currently being loaded.
     */
    suspend fun <T : Network<T>> getNetwork(bridge: NetworkBridge, networkType: NetworkType<T>): ProtoNetwork<T>? =
        getNetworks(bridge)[networkType]?.let { resolveNetwork(it) } as ProtoNetwork<T>?
    
    /**
     * Iterates over all networks of [bridge], calling [action] for each network.
     *
     * @throws IllegalStateException If there is no data for [bridge].
     * @throws IllegalStateException If a referenced network is currently being loaded.
     */
    suspend inline fun forEachNetwork(bridge: NetworkBridge, action: (NetworkType<*>, ProtoNetwork<*>) -> Unit) {
        val networks = getNetworks(bridge)
        for ((networkType, networkId) in networks) {
            val network = resolveNetwork(networkId)
            action(networkType, network)
        }
    }
    
    /**
     * Iterates over all networks of [endPoint], calling [action] for each network.
     * The action parameters describe with which [NetworkType] and through which [BlockFace] the [endPoint]
     * is connected to the given [ProtoNetwork].
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     * @throws IllegalStateException If a referenced network is currently being loaded.
     */
    suspend inline fun forEachNetwork(endPoint: NetworkEndPoint, action: (NetworkType<*>, BlockFace, ProtoNetwork<*>) -> Unit) {
        val networks = getNetworks(endPoint)
        for ((networkType, face, networkId) in networks) {
            val network = resolveNetwork(networkId)
            action(networkType, face, network)
        }
    }
    
    /**
     * Iterates over all [networkType] networks of [endPoint], calling [action] for each network.
     * The action parameters describe through which [BlockFace] the [endPoint] is connected to the given [ProtoNetwork].
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     * @throws IllegalStateException If a referenced network is currently being loaded.
     */
    suspend inline fun <T : Network<T>> forEachNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<T>, action: (BlockFace, ProtoNetwork<T>) -> Unit) {
        val networks = getNetworks(endPoint).row(networkType)
        for ((face, networkId) in networks) {
            val network = resolveNetwork(networkId) as ProtoNetwork<T>
            action(face, network)
        }
    }
    
    /**
     * Gets the supported network types for [bridge].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getSupportedNetworkTypes(bridge: NetworkBridge): MutableSet<NetworkType<*>> =
        getBridgeData(bridge).supportedNetworkTypes
    
    /**
     * Gets the allowed faces for [bridge].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getBridgeFaces(bridge: NetworkBridge): MutableSet<BlockFace> =
        getBridgeData(bridge).bridgeFaces
    
    // ---- More complex utility functions ----
    
    /**
     * Computes a set of allowed [BlockFaces][BlockFace] with which [endPoint]
     * is allowed to connect to [Networks][Network] of the given [type].
     * Will be empty if the [NetworkEndPoint] does not contain all [required holder types][NetworkType.holderTypes].
     */
    fun getAllowedFaces(endPoint: NetworkEndPoint, type: NetworkType<*>): Set<BlockFace> {
        val holders = endPoint.holders
        if (!type.holderTypes.all { requiredType -> holders.any { holder -> requiredType.isSuperclassOf(holder::class) } })
            return emptySet()
        
        return holders.asSequence()
            .filter { holder -> type.holderTypes.any { requiredType -> requiredType.isSuperclassOf(holder::class) } }
            .flatMap { it.allowedFaces }
            .toEnumSet()
    }
    
    /**
     * Gets a set of allowed [BlockFaces][BlockFace] with which [bridge] is allowed
     * to connect to [Networks][Network] of the given [type], which are all supported
     * faces if the [NetworkBridge] supports the [NetworkType].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getAllowedFaces(bridge: NetworkBridge, type: NetworkType<*>): Set<BlockFace> {
        val data = getBridgeData(bridge)
        if (type in data.supportedNetworkTypes)
            return data.bridgeFaces
        return emptySet()
    }
    
    /**
     * Tries to connect [endPoint] to [bridge] from [face] over [networkType],
     * adding all [ProtoNetworks][ProtoNetwork] whose clusters need to be enlarged
     * with [endPoint] to [clustersToEnlarge].
     */
    suspend fun connectEndPointToBridge(
        endPoint: NetworkEndPoint, bridge: NetworkBridge,
        networkType: NetworkType<*>, face: BlockFace,
        clustersToEnlarge: MutableSet<ProtoNetwork<*>>
    ) {
        val oppositeFace = face.oppositeFace
        val network = getNetwork(bridge, networkType)!!
        
        // add to network
        network.addEndPoint(endPoint, face)
        clustersToEnlarge += network
        
        // update state
        setConnection(endPoint, networkType, face)
        setNetwork(endPoint, face, network)
        setConnection(bridge, networkType, oppositeFace)
    }
    
    /**
     * Connects [endPoint] to [other] from [face] over [networkType]
     * and adds the new [ProtoNetwork] to [clustersToInit].
     *
     * @return `true` if the action was successful
     */
    suspend fun <T : Network<T>> connectEndPointToEndPoint(
        endPoint: NetworkEndPoint, other: NetworkEndPoint,
        networkType: NetworkType<T>, face: BlockFace,
        clustersToInit: MutableSet<ProtoNetwork<*>>
    ): Boolean {
        if (!networkType.validateLocal(endPoint, other, face))
            return false
        
        val oppositeFace = face.oppositeFace
        
        // create a new "local" network
        val network = createNetwork(networkType)
        network.addEndPoint(endPoint, face)
        network.addEndPoint(other, oppositeFace)
        clustersToInit += network
        
        // update state
        setConnection(endPoint, networkType, face)
        setNetwork(endPoint, face, network)
        setConnection(other, networkType, oppositeFace)
        setNetwork(other, oppositeFace, network)
        
        return true
    }
    
    /**
     * Disconnects [endPoint] from [bridge] at [face] over [networkType],
     * adding all [ProtoNetworks][ProtoNetwork] whose clusters need to be reinitialized
     * to [clustersToInit].
     */
    suspend fun disconnectEndPointFromBridge(
        endPoint: NetworkEndPoint, bridge: NetworkBridge,
        networkType: NetworkType<*>, face: BlockFace,
        clustersToInit: MutableSet<ProtoNetwork<*>>
    ) {
        removeConnection(endPoint, networkType, face)
        removeConnection(bridge, networkType, face.oppositeFace)
        removeNetwork(endPoint, networkType, face)
        
        val network = getNetwork(bridge, networkType)!!
        if (network.removeFace(endPoint, face)) {
            network.cluster?.forEach { previouslyClusteredNetwork ->
                previouslyClusteredNetwork.invalidateCluster()
                clustersToInit += previouslyClusteredNetwork
            }
        }
    }
    
    /**
     * Disconnects [endPoint] from [other] at [face] over [networkType],
     * adding all [ProtoNetworks][ProtoNetwork] whose clusters need to be reinitialized
     * to [clustersToInit].
     */
    suspend fun disconnectEndPointFromEndPoint(
        endPoint: NetworkEndPoint, other: NetworkEndPoint,
        networkType: NetworkType<*>, face: BlockFace,
        clustersToInit: MutableSet<ProtoNetwork<*>>
    ) {
        val oppositeFace = face.oppositeFace
        
        val network = getNetwork(endPoint, networkType, face)!!
        network.removeNode(endPoint)
        network.removeNode(other)
        
        check(network.isEmpty()) // this must've been a local network, so it should be empty now
        deleteNetwork(network)
        
        removeConnection(endPoint, networkType, face)
        removeConnection(other, networkType, oppositeFace)
        removeNetwork(endPoint, networkType, face)
        removeNetwork(other, networkType, oppositeFace)
        
        network.cluster?.forEach { previouslyClusteredNetwork ->
            previouslyClusteredNetwork.invalidateCluster()
            if (previouslyClusteredNetwork != network) {
                clustersToInit += previouslyClusteredNetwork
            }
        }
    }
    
    /**
     * Performs network connect/disconnect actions based on the new allowed faces
     * of [endPoint] for [networkType] at [face].
     */
    suspend fun handleEndPointAllowedFacesChange(
        endPoint: NetworkEndPoint,
        networkType: NetworkType<*>, face: BlockFace
    ) {
        val clustersToEnlarge = HashSet<ProtoNetwork<*>>()
        val clustersToInit = HashSet<ProtoNetwork<*>>()
        
        val allowedFaces = getAllowedFaces(endPoint, networkType)
        val isCurrentlyConnected = hasConnection(endPoint, networkType, face)
        val neighbor = nodesByPos[endPoint.pos.advance(face)]
        
        if (face in allowedFaces) {
            if (!isCurrentlyConnected) {
                when {
                    neighbor is NetworkBridge && face.oppositeFace in getAllowedFaces(neighbor, networkType) ->
                        connectEndPointToBridge(endPoint, neighbor, networkType, face, clustersToEnlarge)
                    
                    neighbor is NetworkEndPoint && face.oppositeFace in getAllowedFaces(neighbor, networkType) ->
                        connectEndPointToEndPoint(endPoint, neighbor, networkType, face, clustersToInit)
                }
            }
        } else if (isCurrentlyConnected) {
            when (neighbor) {
                is NetworkBridge -> disconnectEndPointFromBridge(endPoint, neighbor, networkType, face, clustersToInit)
                is NetworkEndPoint -> disconnectEndPointFromEndPoint(endPoint, neighbor, networkType, face, clustersToInit)
                null -> Unit
            }
        }
        
        clustersToEnlarge.forEach { it.enlargeCluster(endPoint) }
        clustersToInit.forEach { it.initCluster() }
        
        endPoint.handleNetworkUpdate(this)
        neighbor?.handleNetworkUpdate(this)
    }
    
    internal fun save(scope: CoroutineScope) {
        // save active networks
        for (deferredNetwork in networksById.values) {
            scope.launch(Dispatchers.IO) {
                val network = deferredNetwork.await()
                    ?: return@launch
                save(network)
            }
        }
        
        // save unloaded networks
        val pendingUnloadIterator = pendingUnloadNetworks.iterator()
        while (pendingUnloadIterator.hasNext()) {
            val (_, network) = pendingUnloadIterator.next()
            if (network.isUnloaded()) {
                pendingUnloadIterator.remove()
                scope.launch(Dispatchers.IO) { save(network) }
            }
        }
        
        // delete removed networks
        val pendingRemovalIterator = pendingRemovalNetworks.iterator()
        while (pendingRemovalIterator.hasNext()) {
            val networkId = pendingRemovalIterator.next()
            pendingRemovalIterator.remove()
            scope.launch(Dispatchers.IO) {
                val file = File(storage.networkFolder, "$networkId.nvnt")
                file.delete()
            }
        }
    }
    
    private fun save(network: ProtoNetwork<*>) {
        val file = File(storage.networkFolder, "${network.uuid}.nvnt")
        file.outputStream().buffered().use { out ->
            val writer = ByteWriter.fromStream(out)
            ProtoNetwork.write(network, writer)
        }
    }
    
}