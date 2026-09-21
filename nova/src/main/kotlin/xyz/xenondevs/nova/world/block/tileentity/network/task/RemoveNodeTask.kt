package xyz.xenondevs.nova.world.block.tileentity.network.task

import xyz.xenondevs.nova.world.block.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.node.safelyHandleNetworkUpdate
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.format.NetworkState

internal abstract class RemoveNodeTask<T : NetworkNode>(
    state: NetworkState,
    val node: T,
    private val updateNodes: Boolean
) : NetworkTask(state) {
    
    override val chunkPos = node.block.chunkPos
    
    protected val nodesToUpdate = HashSet<NetworkNode>()
    
    final override suspend fun run(): Boolean {
        if (node !in state)
            return false
        
        state -= node
        remove()
        state.removeNodeData(node)
        
        if (updateNodes) {
            for (node in nodesToUpdate) {
                node.safelyHandleNetworkUpdate(state)
            }
        }
        
        return true
    }
    
    abstract suspend fun remove()
    
    /**
     * Invalidates the cluster of all [ProtoNetworks][ProtoNetwork] clustered with [network].
     */
    protected fun invalidateCluster(network: ProtoNetwork<*>) =
        network.cluster?.invalidate()
    
}
