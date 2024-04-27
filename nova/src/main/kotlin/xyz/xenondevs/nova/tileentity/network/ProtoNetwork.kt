package xyz.xenondevs.nova.tileentity.network

import org.bukkit.World
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.node.GhostNetworkNode
import xyz.xenondevs.nova.tileentity.network.node.MutableNetworkNodeConnection
import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.chunk.readCubeFaceSet
import xyz.xenondevs.nova.world.format.chunk.writeCubeFaceSet
import java.util.*

/**
 * An uninitialized network that is still in creation.
 */
class ProtoNetwork(
    private val state: NetworkState,
    override val type: NetworkType,
    override val uuid: UUID = UUID.randomUUID(),
    override val nodes: MutableMap<BlockPos, MutableNetworkNodeConnection> = HashMap()
) : NetworkData {
    
    /**
     * The [ProtoNetworkCluster] that this [ProtoNetwork] is a part of.
     */
    var cluster: ProtoNetworkCluster? = null
        private set
    
    /**
     * The [Network] that has been built from this [ProtoNetwork].
     * @see dirty
     */
    lateinit var network: Network
    
    /**
     * Whether this [ProtoNetwork] has been modified since the last [network] was built from it.
     */
    var dirty = true
    
    /**
     * Adds all [ProtoNetwork.nodes] of the given [ProtoNetwork] to this [ProtoNetwork].
     * Should only be called for [ProtoNetworks][ProtoNetwork] of the same [type][NetworkData.type].
     */
    fun addAll(network: NetworkData) {
        require(network.type == type) { "Type mismatch" }
        for ((node, faces) in network.nodes.values) {
            val (_, myFaces) = this.nodes.getOrPut(node.pos) { MutableNetworkNodeConnection(node) }
            myFaces += faces
        }
        dirty = true
    }
    
    /**
     * Adds a [bridge] to this [ProtoNetwork].
     */
    fun addBridge(bridge: NetworkBridge) {
        nodes[bridge.pos] = MutableNetworkNodeConnection(bridge, Collections.emptySet())
        dirty = true
    }
    
    /**
     * Adds [face] to the [NetworkEndPoint] at [endPoint.pos][NetworkEndPoint.pos],
     * or adds [endPoint] and [face] to the [ProtoNetwork].
     *
     * @return - `true` if the [endPoint] was added to the [ProtoNetwork]
     * - `false` if there already was a [NetworkEndPoint] at [NetworkEndPoint.pos]
     */
    fun addEndPoint(endPoint: NetworkNode, face: BlockFace): Boolean {
        val presentFaces = nodes[endPoint.pos]?.faces
        if (presentFaces != null) {
            if (presentFaces.add(face)) {
                dirty = true
            }
            return false
        } else {
            nodes[endPoint.pos] = MutableNetworkNodeConnection(endPoint, EnumSet.of(face))
            dirty = true
            return true
        }
    }
    
    /**
     * Adds [faces] to the [NetworkEndPoint] at [endPoint.pos][NetworkEndPoint.pos],
     * or adds [endPoint] and [faces] to the [ProtoNetwork].
     *
     * @return - `true` if the [endPoint] was added to the [ProtoNetwork]
     * - `false` if there already was a [NetworkEndPoint] at [NetworkEndPoint.pos]
     */
    fun addEndPoint(endPoint: NetworkNode, faces: Set<BlockFace>): Boolean {
        val presentFaces = nodes[endPoint.pos]?.faces
        if (presentFaces != null) {
            if (presentFaces.addAll(faces)) {
                dirty = true
            }
            return false
        } else {
            nodes[endPoint.pos] = MutableNetworkNodeConnection(endPoint, faces.toEnumSet())
            dirty = true
            return true
        }
    }
    
    /**
     * Remove the [NetworkNode] at [node.pos][NetworkNode.pos] from this [ProtoNetwork].
     *
     * @return - `true` if the [node] was removed
     * - `false` if there was no [NetworkNode] at [node.pos][NetworkNode.pos]
     */
    fun removeNode(node: NetworkNode): Boolean {
        if (nodes.remove(node.pos) != null) {
            dirty = true
            return true
        }
        return false
    }
    
    /**
     * Removes a [face] through which the [NetworkEndPoint] at
     * [endPoint.pos][NetworkEndPoint.pos] connects to this [ProtoNetwork].
     *
     * @return - `true` if [endPoint] was completely removed from this [ProtoNetwork]
     * - `false` if [endPoint] is still connected through other faces
     */
    fun removeFace(endPoint: NetworkEndPoint, face: BlockFace): Boolean {
        val presentFaces = nodes[endPoint.pos]?.faces
            ?: return false
        
        if (presentFaces.remove(face)) {
            dirty = true
        }
        
        if (presentFaces.isEmpty()) {
            nodes -= endPoint.pos
            return true
        }
        
        return false
    }
    
    /**
     * Removes all [nodes] from this [NetworkData].
     */
    fun removeAll(nodes: Set<NetworkNode>) {
        for (node in nodes) {
            this.nodes -= node.pos
        }
        dirty = true
    }
    
    /**
     * Replaces a ghost node with the regular [node].
     *
     * @throws IllegalStateException If there is no ghost node for [node].
     */
    fun loadNode(node: NetworkNode) {
        val connection = nodes[node.pos]
            ?: throw IllegalStateException("No node present at ${node.pos}")
        
        nodes[node.pos] = connection.copy(node = node)
        dirty = true
    }
    
    /**
     * Replaces [node] with a ghost node.
     */
    fun unloadNode(node: NetworkNode) {
        val connection = nodes.remove(node.pos)
            ?: return
        
        nodes[node.pos] = connection.copy(node = GhostNetworkNode.fromNode(node))
        dirty = true
    }
    
    /**
     * Checks whether this [ProtoNetwork] is empty.
     * A [ProtoNetwork] is considered empty if it has no [NetworkBridge] and
     * less than two [NetworkEndPoints][NetworkEndPoint].
     */
    fun isEmpty(): Boolean {
        return nodes.size < 2 && nodes.none { (_, con) -> con.node is NetworkBridge }
    }
    
    /**
     * Checks whether all nodes in this [ProtoNetwork] are unloaded.
     */
    fun isUnloaded(): Boolean {
        return nodes.values.all { it.node is GhostNetworkNode }
    }
    
    /**
     * Completely builds the [cluster] based on [nodes].
     * Building a cluster also initializes / updates the clusters of all
     * [ProtoNetworks][ProtoNetwork] that are clustered with it.
     *
     * Does nothing if the [cluster] has already been built.
     */
    fun initCluster() {
        if (cluster != null)
            return
        
        println("Building cluster for network $uuid ($type)")
        
        val cluster = ProtoNetworkCluster()
        val queue = LinkedList<ProtoNetwork>()
        queue += this
        processClusterQueue(cluster, queue)
        this.cluster = cluster
    }
    
    
    /**
     * Enlarges the [cluster] using the [ProtoNetworks][ProtoNetwork] of [node].
     * Enlarging a cluster also initializes / updates the clusters of all
     * [ProtoNetworks][ProtoNetwork] that are clustered with it.
     *
     * If no [cluster] has been built yet, [initCluster] will be called instead.
     */
    fun enlargeCluster(node: NetworkNode) {
        val cluster = cluster ?: return initCluster()
        println("Enlarging cluster for network $uuid ($type)")
        
        val queue = LinkedList<ProtoNetwork>()
        queueWithRelatedNetworks(cluster, queue, node)
        processClusterQueue(cluster, queue)
    }
    
    /**
     * Enlarges the [cluster] using the [ProtoNetworks][ProtoNetwork] of [nodes].
     * Enlarging a cluster also initializes / updates the clusters of all
     * [ProtoNetworks][ProtoNetwork] that are clustered with it.
     *
     * If no [cluster] has been built yet, [initCluster] will be called instead.
     */
    fun enlargeCluster(nodes: Collection<NetworkNode>) {
        val cluster = cluster ?: return initCluster()
        if (nodes.isEmpty())
            return
        
        println("Enlarging cluster for network $uuid ($type)")
        
        val queue = LinkedList<ProtoNetwork>()
        for (node in nodes) {
            queueWithRelatedNetworks(cluster, queue, node)
        }
        processClusterQueue(cluster, queue)
    }
    
    private fun processClusterQueue(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork>) {
        while (queue.isNotEmpty()) {
            val network = queue.poll()
            if (network in cluster)
                continue
            
            cluster += network
            network.cluster = cluster
            
            for ((node, _) in network.nodes.values) {
                queueWithRelatedNetworks(cluster, queue, node)
            }
        }
    }
    
    private fun queueWithRelatedNetworks(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork>, node: NetworkNode) {
        queueNetworks(cluster, queue, node)
        for (relatedNode in node.linkedNodes) {
            queueNetworks(cluster, queue, relatedNode)
        }
    }
    
    private fun queueNetworks(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork>, node: NetworkNode) {
        // ghost nodes do not affect clustering because they're unloaded
        if (node is GhostNetworkNode)
            return
        
        when (node) {
            is NetworkEndPoint -> {
                for ((_, _, otherNetworkId) in state.getNetworks(node)) {
                    val otherNetwork = state.resolveNetwork(otherNetworkId)
                    if (otherNetwork !in cluster)
                        queue += otherNetwork
                }
            }
            
            is NetworkBridge -> {
                for ((_, otherNetworkId) in state.getNetworks(node)) {
                    val otherNetwork = state.resolveNetwork(otherNetworkId)
                    if (otherNetwork !in cluster)
                        queue += otherNetwork
                }
            }
        }
    }
    
    /**
     * Invalidates the [cluster] of this [ProtoNetwork], requiring it to be rebuilt via [initCluster].
     */
    fun invalidateCluster() {
        cluster = null
    }
    
    /**
     * Creates an immutable copy of this [ProtoNetwork].
     */
    fun immutableCopy(): NetworkData =
        ImmutableNetworkData(
            type, uuid,
            nodes.mapValuesTo(HashMap()) { (_, con) ->
                con.copy(faces = con.faces.toEnumSet())
            }
        )
    
    companion object {
        
        /**
         * Reads a [ProtoNetwork] from the given [reader].
         *
         * May suspend to wait for network region load.
         */
        internal suspend fun read(uuid: UUID, world: World, state: NetworkState, reader: ByteReader): ProtoNetwork {
            val type = NovaRegistries.NETWORK_TYPE.getOrThrow(reader.readString())
            val size = reader.readVarInt()
            val nodes = HashMap<BlockPos, MutableNetworkNodeConnection>()
            for (i in 0 until size) {
                val pos = BlockPos(
                    world,
                    reader.readVarInt(),
                    reader.readVarInt(),
                    reader.readVarInt()
                )
                
                nodes[pos] = MutableNetworkNodeConnection(
                    GhostNetworkNode.fromData(pos, state.getOrLoadNodeData(pos)),
                    reader.readCubeFaceSet()
                )
            }
            return ProtoNetwork(state, type, uuid, nodes)
        }
        
        /**
         * Writes the given [network] to the [writer].
         */
        internal fun write(network: ProtoNetwork, writer: ByteWriter) {
            writer.writeString(network.type.toString())
            writer.writeVarInt(network.nodes.size)
            for ((pos, connection) in network.nodes) {
                writer.writeVarInt(pos.x)
                writer.writeVarInt(pos.y)
                writer.writeVarInt(pos.z)
                writer.writeCubeFaceSet(connection.faces)
            }
        }
        
    }
    
}