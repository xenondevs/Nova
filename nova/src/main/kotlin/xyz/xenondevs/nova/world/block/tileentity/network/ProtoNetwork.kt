package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.GhostNetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNodeConnection
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.format.NetworkState
import java.util.*

/**
 * An uninitialized network that is still in creation.
 */
class ProtoNetwork<T : Network<T>>(
    private val state: NetworkState,
    override val type: NetworkType<T>,
    override val uuid: UUID = UUID.randomUUID(),
    override val nodes: MutableMap<Block, NetworkNodeConnection> = HashMap()
) : NetworkData<T> {
    
    /**
     * The [ProtoNetworkCluster] that this [ProtoNetwork] is a part of.
     */
    var cluster: ProtoNetworkCluster? = null
        private set
    
    /**
     * The [Network] that has been built from this [ProtoNetwork].
     * @see dirty
     */
    lateinit var network: Network<T>
    
    /**
     * Whether this [ProtoNetwork] has been modified since the last [network] was built from it.
     */
    var dirty = true
        private set
    
    /**
     * Adds all [ProtoNetwork.nodes] of the given [ProtoNetwork] to this [ProtoNetwork].
     * Should only be called for [ProtoNetworks][ProtoNetwork] of the same [type][NetworkData.type].
     */
    fun addAll(network: NetworkData<T>) {
        for ((node, faces) in network.nodes.values) {
            require(node !is GhostNetworkNode)
            nodes.compute(node.block) { _, connection ->
                val connection = connection ?: NetworkNodeConnection(node)
                connection.copy(faces = connection.faces + faces)
            }
        }
        markDirty()
    }
    
    /**
     * Adds a [bridge] to this [ProtoNetwork].
     */
    fun addBridge(bridge: NetworkBridge) {
        require(bridge !is GhostNetworkNode)
        nodes[bridge.block] = NetworkNodeConnection(bridge)
        markDirty()
    }
    
    /**
     * Adds [face] to the [NetworkEndPoint] at [endPoint.pos][NetworkEndPoint.block],
     * or adds [endPoint] and [face] to the [ProtoNetwork].
     *
     * @return - `true` if the [endPoint] was added to the [ProtoNetwork]
     * - `false` if there already was a [NetworkEndPoint] at [NetworkEndPoint.block]
     */
    fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace): Boolean =
        addEndPoint(endPoint, CubeFaceSet.NONE + face)
    
    /**
     * Adds [faces] to the [NetworkEndPoint] at [endPoint.pos][NetworkEndPoint.block],
     * or adds [endPoint] and [faces] to the [ProtoNetwork].
     *
     * @return - `true` if the [endPoint] was added to the [ProtoNetwork]
     * - `false` if there already was a [NetworkEndPoint] at [NetworkEndPoint.block]
     */
    fun addEndPoint(endPoint: NetworkEndPoint, faces: CubeFaceSet): Boolean {
        require(endPoint !is GhostNetworkNode)
        require(faces.isNotEmpty())
        val connection = nodes[endPoint.block]
        if (connection != null) {
            val updatedFaces = connection.faces + faces
            if (updatedFaces != connection.faces) {
                nodes[endPoint.block] = connection.copy(faces = updatedFaces)
                markDirty()
            }
            return false
        } else {
            nodes[endPoint.block] = NetworkNodeConnection(endPoint, faces)
            markDirty()
            return true
        }
    }
    
    /**
     * Remove the [NetworkNode] at [node.pos][NetworkNode.block] from this [ProtoNetwork].
     *
     * @return - `true` if the [node] was removed
     * - `false` if there was no [NetworkNode] at [node.pos][NetworkNode.block]
     */
    fun removeNode(node: NetworkNode): Boolean {
        if (nodes.remove(node.block) != null) {
            markDirty()
            return true
        }
        return false
    }
    
    /**
     * Removes a [face] through which the [NetworkEndPoint] at
     * [endPoint.pos][NetworkEndPoint.block] connects to this [ProtoNetwork].
     *
     * @return - `true` if [endPoint] was completely removed from this [ProtoNetwork]
     * - `false` if [endPoint] is still connected through other faces
     */
    fun removeFace(endPoint: NetworkEndPoint, face: BlockFace): Boolean {
        val connection = nodes[endPoint.block]
            ?: return false
        val updatedFaces = connection.faces - face
        
        if (updatedFaces == connection.faces)
            return false
        if (updatedFaces.isEmpty()) {
            nodes -= endPoint.block
            markDirty()
            return true
        }
        
        nodes[endPoint.block] = connection.copy(faces = updatedFaces)
        markDirty()
        return false
    }
    
    /**
     * Removes all [nodes] from this [NetworkData].
     */
    fun removeAll(nodes: Collection<NetworkNode>) {
        for (node in nodes) {
            this.nodes -= node.block
        }
        markDirty()
    }
    
    /**
     * Checks whether this [ProtoNetwork] is empty.
     */
    fun isEmpty(): Boolean {
        return nodes.isEmpty()
    }
    
    /**
     * Completely builds the [cluster] based on [nodes].
     * Building a cluster also initializes / updates the clusters of all
     * [ProtoNetworks][ProtoNetwork] that are clustered with it.
     *
     * Does nothing if the [cluster] has already been built.
     */
    suspend fun initCluster(): ProtoNetworkCluster {
        if (cluster != null)
            return cluster!!
        
        val cluster = ProtoNetworkCluster()
        val queue = ArrayDeque<ProtoNetwork<*>>()
        queue += this
        processClusterQueue(cluster, queue)
        this.cluster = cluster
        return cluster
    }
    
    private suspend fun processClusterQueue(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork<*>>) {
        while (queue.isNotEmpty()) {
            val network = queue.poll()
            if (network in cluster)
                continue
            
            cluster += network
            network.cluster = cluster
            
            for ([node, _] in network.nodes.values) {
                queueWithRelatedNetworks(cluster, queue, node)
            }
        }
    }
    
    private suspend fun queueWithRelatedNetworks(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork<*>>, node: NetworkNode) {
        queueNetworks(cluster, queue, node)
        for (linkedNode in node.linkedNodes) {
            if (linkedNode !in state)
                continue
            queueNetworks(cluster, queue, linkedNode)
        }
    }
    
    private suspend fun queueNetworks(cluster: ProtoNetworkCluster, queue: Queue<ProtoNetwork<*>>, node: NetworkNode) {
        // ghost nodes do not affect clustering because they're unloaded
        if (node is GhostNetworkNode)
            return
        
        when (node) {
            is NetworkEndPoint -> {
                for ([otherNetworkType, _, otherNetworkId] in state.getNetworks(node)) {
                    val otherNetwork = state.getNetworkOrThrow(otherNetworkType, otherNetworkId)
                    if (otherNetwork !in cluster)
                        queue += otherNetwork
                }
            }
            
            is NetworkBridge -> {
                for ([otherNetworkType, otherNetworkId] in state.getNetworks(node)) {
                    val otherNetwork = state.getNetworkOrThrow(otherNetworkType, otherNetworkId)
                    if (otherNetwork !in cluster)
                        queue += otherNetwork
                }
            }
        }
    }
    
    /**
     * Unsets the [ProtoNetwork.cluster] iff it matches [cluster].
     */
    fun invalidateCluster(cluster: ProtoNetworkCluster) {
        if (this.cluster === cluster)
            this.cluster = null
    }
    
    /**
     * Creates an immutable copy of this [ProtoNetwork].
     */
    fun immutableCopy(): NetworkData<T> =
        ImmutableNetworkData(type, uuid, HashMap(nodes))
    
    /**
     * Marks this [ProtoNetwork] and its [cluster] as dirty,
     * requiring them to be rebuilt.
     */
    fun markDirty() {
        dirty = true
        cluster?.dirty = true
    }
    
    /**
     * Marks this [ProtoNetwork] as clean, indicating that it has been rebuilt.
     */
    internal fun markClean() {
        dirty = false
    }
    
    override fun toString(): String {
        return "ProtoNetwork(type=$type, uuid=$uuid, nodes=$nodes)"
    }
    
}
