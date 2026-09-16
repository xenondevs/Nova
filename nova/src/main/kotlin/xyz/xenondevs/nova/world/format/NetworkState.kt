@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.world.format

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import kotlinx.coroutines.sync.Mutex
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.commons.guava.set
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.GhostNetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.format.chunk.NetworkBridgeData
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import xyz.xenondevs.nova.world.format.chunk.NetworkEndPointData
import xyz.xenondevs.nova.world.format.chunk.NetworkNodeData
import java.util.*

/**
 * Contains or links to all network-related data for a specific [World].
 *
 * Unless otherwise specified, all functions in this class are only to be
 * called from the network configurator context.
 */
class NetworkState internal constructor(
    internal val storage: RegionFileStorage<NetworkChunk, NetworkRegionFile>
) {
    
    private val networksById = HashMap<UUID, ProtoNetwork<*>>()
    
    private val nodesByChunk = HashMap<ChunkPos, MutableMap<Block, NetworkNode>>()
    
    /**
     * Mutex for all data governed by this [NetworkState].
     */
    internal val mutex = Mutex()
    
    /**
     * A sequence of all [ProtoNetworks][ProtoNetwork].
     */
    val networks: Sequence<ProtoNetwork<*>>
        get() = networksById.values.asSequence()
    
    /**
     * Adds [node] to the network state.
     */
    operator fun plusAssign(node: NetworkNode) {
        val nodes = nodesByChunk.getOrPut(node.block.chunkPos, ::HashMap)
        nodes[node.block] = node
    }
    
    /**
     * Removes [node] from the network state.
     */
    operator fun minusAssign(node: NetworkNode) {
        val chunkPos = node.block.chunkPos
        val nodes = nodesByChunk[chunkPos]
            ?: return
        nodes -= node.block
        if (nodes.isEmpty())
            nodesByChunk -= chunkPos
    }
    
    /**
     * Adds [network] to the network state.
     */
    operator fun plusAssign(network: ProtoNetwork<*>) {
        networksById[network.uuid] = network
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
     * Removes the [network] from the network state.
     */
    operator fun minusAssign(network: ProtoNetwork<*>) {
        networksById -= network.uuid
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
        getNode(node.block) != null
    
    /**
     * Gets the loaded network node at [block], or null if there is none.
     */
    fun getNode(block: Block): NetworkNode? =
        nodesByChunk[block.chunkPos]?.get(block)
    
    /**
     * Gets all loaded network nodes in the chunk at [chunkPos].
     */
    fun getNodes(chunkPos: ChunkPos): Collection<NetworkNode> =
        nodesByChunk[chunkPos]?.values ?: emptyList()
    
    /**
     * Removes and returns all loaded network nodes in the chunk at [chunkPos].
     */
    fun removeNodes(chunkPos: ChunkPos): Map<Block, NetworkNode> =
        nodesByChunk.remove(chunkPos) ?: emptyMap()
    
    /**
     * Gets the [ProtoNetwork] with the given [networkId] and [type], or throws an exception
     * if no such network exists.
     *
     * @throws IllegalArgumentException If no network with [networkId] exists, or it is not of [type].
     */
    fun <T : Network<T>> getNetworkOrThrow(type: NetworkType<T>, networkId: UUID): ProtoNetwork<T> {
        val network = networksById[networkId]
        if (network == null)
            throw IllegalArgumentException("Network with id $networkId does not exist")
        if (network.type != type)
            throw IllegalArgumentException("Network with id $networkId is not of type $type")
        return network as ProtoNetwork<T>
    }
    
    /**
     * Creates a new [ProtoNetwork] with the given [type] and a random UUID.
     */
    fun <T : Network<T>> createNetwork(type: NetworkType<T>): ProtoNetwork<T> {
        val networkId = UUID.randomUUID()
        val network = ProtoNetwork(this, type, networkId)
        networksById[networkId] = network
        return network
    }
    
    /**
     * Gets or creates a [ProtoNetwork] with the given [type] and [networkId].
     *
     * @throws IllegalArgumentException If a network with the same [networkId] exists, but is not of the given [type].
     */
    fun <T : Network<T>> getOrCreateNetwork(type: NetworkType<T>, networkId: UUID = UUID.randomUUID()): ProtoNetwork<T> {
        val network = networksById.computeIfAbsent(networkId) { ProtoNetwork(this, type, networkId) }
        if (network.type != type)
            throw IllegalArgumentException("Network with id $networkId exists, but is not of type $type")
        return network as ProtoNetwork<T>
    }
    
    /**
     * Resolves a [NetworkNode] by its [block].
     *
     * @throws IllegalStateException If there is no data for a node at [block].
     */
    suspend fun resolveNode(block: Block): NetworkNode {
        val node = getNode(block)
        if (node != null)
            return node
        
        return GhostNetworkNode.fromData(block, this.getNodeData(block))
    }
    
    /**
     * Finds all nearby [NetworkNodes][NetworkNode] of [block] using the given [faces].
     */
    fun getNearbyNodes(block: Block, faces: CubeFaceSet): CubeFaceMap<NetworkNode?> =
        faces.associateWith { face -> getNode(block.getRelative(face)) }
    
    /**
     * Runs [action] for each nearby [NetworkNode] of [block] using the given [faces].
     */
    inline fun forEachNearbyNode(
        block: Block,
        faces: CubeFaceSet,
        action: (face: BlockFace, neighbor: NetworkNode) -> Unit
    ) {
        faces.forEach { face ->
            val neighbor = getNode(block.getRelative(face))
            if (neighbor != null)
                action(face, neighbor)
        }
    }
    
    // ---- Data ----
    
    /**
     * Gets all network node data for the given [pos].
     */
    suspend fun getNodeData(pos: ChunkPos): Map<Block, NetworkNodeData> =
        storage.getOrLoadRegionizedChunk(pos).getData()
    
    /**
     * Gets the [NetworkNodeData] for [block], potentially loading the corresponding
     * network region if necessary and throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for a node at [block].
     */
    suspend fun getNodeData(block: Block): NetworkNodeData =
        storage.getOrLoadRegionizedChunk(block.chunkPos).getData(block)
            ?: throw IllegalStateException("No data for node at $block")
    
    /**
     * Gets the [NetworkNodeData] for [node], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun getNodeData(node: NetworkNode): NetworkNodeData =
        this.getNodeData(node.block)
    
    /**
     * Gets the [NetworkBridgeData] for [bridge], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getBridgeData(bridge: NetworkBridge): NetworkBridgeData =
        storage.getOrLoadRegionizedChunk(bridge.block.chunkPos).getBridgeData(bridge.block)
            ?: throw IllegalStateException("No data for bridge at ${bridge.block}")
    
    /**
     * Sets [data] at [block].
     */
    suspend fun setBridgeData(block: Block, data: NetworkBridgeData) {
        storage.getOrLoadRegionizedChunk(block.chunkPos).setBridgeData(block, data)
    }
    
    /**
     * Gets the [NetworkEndPointData] for [endPoint], or throws an exception if there is no data.
     *
     * @throws IllegalStateException If there is no data for [endPoint].
     */
    suspend fun getEndPointData(endPoint: NetworkEndPoint): NetworkEndPointData =
        storage.getOrLoadRegionizedChunk(endPoint.block.chunkPos).getEndPointData(endPoint.block)
            ?: throw IllegalStateException("No data for endpoint at ${endPoint.block}")
    
    /**
     * Sets [data] at [block].
     */
    suspend fun setEndPointData(block: Block, data: NetworkEndPointData) {
        storage.getOrLoadRegionizedChunk(block.chunkPos).setEndPointData(block, data)
    }
    
    /**
     * Removes the data for [node].
     *
     * @see getNodeData
     * @see getEndPointData
     * @see getBridgeData
     */
    suspend fun removeNodeData(node: NetworkNode) {
        storage.getOrLoadRegionizedChunk(node.block.chunkPos).setData(node.block, null)
    }
    
    /**
     * Remembers a connection of [node] at [face] using [networkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun setConnection(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace) {
        val data = getNodeData(node)
        data.connections[networkType] = (data.connections[networkType] ?: CubeFaceSet.NONE) + face
    }
    
    /**
     * Forgets a connection of [node] at [face] using [networkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend fun removeConnection(node: NetworkNode, networkType: NetworkType<*>, face: BlockFace) {
        val data = getNodeData(node)
        data.connections[networkType] = (data.connections[networkType] ?: CubeFaceSet.NONE) - face
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
        if (getNodeData(node).connections.none { [_, faces] -> face in faces })
            return null
        
        return resolveNode(node.block.getRelative(face))
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
        
        return resolveNode(node.block.getRelative(face))
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
        getNodeData(node).connections.any { [_, faces] -> face in faces }
    
    /**
     * Iterates over all [NetworkNodes][NetworkNode] connected to [node], calling [action] for each connection.
     * The action parameters describe using which [NetworkType] and through wich [BlockFace] the [NetworkNode] is connected.
     * Some [NetworkNodes][NetworkNode] may be connected through multiple [NetworkTypes][NetworkType].
     *
     * @throws IllegalStateException If there is no data for [node].
     */
    suspend inline fun forEachConnectedNode(node: NetworkNode, action: (NetworkType<*>, BlockFace, NetworkNode) -> Unit) {
        val connections = getNodeData(node).connections
        for ([networkType, faces] in connections) {
            faces.forEach { face ->
                val connectedNode = resolveNode(node.block.getRelative(face))
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
        faces.forEach { face ->
            val connectedNode = resolveNode(node.block.getRelative(face))
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
    suspend fun setNetwork(endPoint: NetworkEndPoint, faces: CubeFaceSet, network: ProtoNetwork<*>) {
        val data = getEndPointData(endPoint)
        val type = network.type
        val uuid = network.uuid
        faces.forEach { face ->
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
    suspend fun removeNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<*>, faces: CubeFaceSet) {
        val data = getEndPointData(endPoint)
        faces.forEach { face ->
            data.networks.remove(networkType, face)
        }
    }
    
    /**
     * Gets the network of [endPoint] at [face], or `null` if there is no connection.
     */
    suspend fun <T : Network<T>> getNetwork(endPoint: NetworkEndPoint, networkType: NetworkType<T>, face: BlockFace): ProtoNetwork<T>? =
        getEndPointData(endPoint).networks[networkType, face]?.let { getNetworkOrThrow(networkType, it) }
    
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
        getNetworks(bridge)[networkType]?.let { getNetworkOrThrow(networkType, it) }
    
    /**
     * Iterates over all networks of [bridge], calling [action] for each network.
     *
     * @throws IllegalStateException If there is no data for [bridge].
     * @throws IllegalStateException If a referenced network is currently being loaded.
     */
    suspend inline fun forEachNetwork(bridge: NetworkBridge, action: (NetworkType<*>, ProtoNetwork<*>) -> Unit) {
        val networks = getNetworks(bridge)
        for ([networkType, networkId] in networks) {
            val network = getNetworkOrThrow(networkType, networkId)
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
        for ([networkType, face, networkId] in networks) {
            val network = getNetworkOrThrow(networkType, networkId)
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
        for ([face, networkId] in networks) {
            val network = getNetworkOrThrow(networkType, networkId)
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
    suspend fun getBridgeFaces(bridge: NetworkBridge): CubeFaceSet =
        getBridgeData(bridge).bridgeFaces
    
    // ---- More complex utility functions ----
    
    /**
     * Computes a set of allowed [BlockFaces][BlockFace] with which [endPoint]
     * is allowed to connect to [Networks][Network] of the given [type].
     * Will be empty if the [NetworkEndPoint] does not support the given [type], which is implied by [NetworkType.extractHolders] returning `null`.
     */
    fun getAllowedFaces(endPoint: NetworkEndPoint, type: NetworkType<*>): CubeFaceSet {
        var allowedFaces = CubeFaceSet.ALL
        val holders = type.extractHolders(endPoint)
            ?: return CubeFaceSet.NONE
        for (holder in holders) {
            allowedFaces = allowedFaces and holder.allowedFaces
        }
        return allowedFaces
    }
    
    /**
     * Gets a set of allowed [BlockFaces][BlockFace] with which [bridge] is allowed
     * to connect to [Networks][Network] of the given [type], which are all supported
     * faces if the [NetworkBridge] supports the [NetworkType].
     *
     * @throws IllegalStateException If there is no data for [bridge].
     */
    suspend fun getAllowedFaces(bridge: NetworkBridge, type: NetworkType<*>): CubeFaceSet {
        val data = getBridgeData(bridge)
        if (type in data.supportedNetworkTypes)
            return data.bridgeFaces
        return CubeFaceSet.NONE
    }
    
    /**
     * Tries to connect [endPoint] to [bridge] from [face] over [networkType].
     */
    suspend fun connectEndPointToBridge(
        endPoint: NetworkEndPoint, bridge: NetworkBridge,
        networkType: NetworkType<*>, face: BlockFace
    ) {
        val oppositeFace = face.oppositeFace
        val network = getNetwork(bridge, networkType)!!
        
        // add to network
        if (network.addEndPoint(endPoint, face))
            network.cluster?.invalidate()
        
        // update state
        setConnection(endPoint, networkType, face)
        setNetwork(endPoint, face, network)
        setConnection(bridge, networkType, oppositeFace)
    }
    
    /**
     * Connects [endPoint] to [other] from [face] over [networkType].
     *
     * @return `true` if the action was successful
     */
    suspend fun <T : Network<T>> connectEndPointToEndPoint(
        endPoint: NetworkEndPoint, other: NetworkEndPoint,
        networkType: NetworkType<T>, face: BlockFace
    ): Boolean {
        if (!endPoint.requestsLocalNetwork(face) && !other.requestsLocalNetwork(face.oppositeFace))
            return false
        if (!networkType.validateLocal(endPoint, other, face))
            return false
        
        val oppositeFace = face.oppositeFace
        
        // create a new "local" network
        val network = createNetwork(networkType)
        network.addEndPoint(endPoint, face)
        network.addEndPoint(other, oppositeFace)
        
        // update state
        setConnection(endPoint, networkType, face)
        setNetwork(endPoint, face, network)
        setConnection(other, networkType, oppositeFace)
        setNetwork(other, oppositeFace, network)
        
        return true
    }
    
    /**
     * Disconnects [endPoint] from [bridge] at [face] over [networkType].
     */
    suspend fun disconnectEndPointFromBridge(
        endPoint: NetworkEndPoint, bridge: NetworkBridge,
        networkType: NetworkType<*>, face: BlockFace
    ) {
        removeConnection(endPoint, networkType, face)
        removeConnection(bridge, networkType, face.oppositeFace)
        removeNetwork(endPoint, networkType, face)
        
        val network = getNetwork(bridge, networkType)!!
        if (network.removeFace(endPoint, face))
            network.cluster?.invalidate()
    }
    
    /**
     * Disconnects [endPoint] from [other] at [face] over [networkType].
     */
    suspend fun disconnectEndPointFromEndPoint(
        endPoint: NetworkEndPoint, other: NetworkEndPoint,
        networkType: NetworkType<*>, face: BlockFace
    ) {
        val oppositeFace = face.oppositeFace
        
        val network = getNetwork(endPoint, networkType, face)!!
        network.removeNode(endPoint)
        network.removeNode(other)
        
        check(network.isEmpty()) // this must've been a local network, so it should be empty now
        this -= network
        
        removeConnection(endPoint, networkType, face)
        removeConnection(other, networkType, oppositeFace)
        removeNetwork(endPoint, networkType, face)
        removeNetwork(other, networkType, oppositeFace)
        
        network.cluster?.invalidate()
    }
    
    /**
     * Performs network connect/disconnect actions based on the new allowed faces
     * of [endPoint] for [networkType] at [face].
     */
    suspend fun handleEndPointAllowedFacesChange(
        endPoint: NetworkEndPoint,
        networkType: NetworkType<*>, face: BlockFace
    ) {
        val allowedFaces = getAllowedFaces(endPoint, networkType)
        val isCurrentlyConnected = hasConnection(endPoint, networkType, face)
        val neighbor = getNode(endPoint.block.getRelative(face))
        
        if (face in allowedFaces) {
            if (!isCurrentlyConnected) {
                when {
                    neighbor is NetworkBridge && face.oppositeFace in getAllowedFaces(neighbor, networkType) ->
                        connectEndPointToBridge(endPoint, neighbor, networkType, face)
                    
                    neighbor is NetworkEndPoint && face.oppositeFace in getAllowedFaces(neighbor, networkType) ->
                        connectEndPointToEndPoint(endPoint, neighbor, networkType, face)
                }
            }
        } else if (isCurrentlyConnected) {
            when (neighbor) {
                is NetworkBridge -> disconnectEndPointFromBridge(endPoint, neighbor, networkType, face)
                is NetworkEndPoint -> disconnectEndPointFromEndPoint(endPoint, neighbor, networkType, face)
                null -> Unit
            }
        }
        
        endPoint.handleNetworkUpdate(this)
        neighbor?.handleNetworkUpdate(this)
    }
    
}
