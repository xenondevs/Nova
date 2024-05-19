package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.nova.tileentity.network.ProtoNetwork
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.format.NetworkState

internal abstract class RemoveNodeTask<T : NetworkNode>(
    state: NetworkState,
    val node: T,
    private val updateNodes: Boolean
) : NetworkTask(state) {
    
    protected val nodesToUpdate = HashSet<NetworkNode>()
    protected val clustersToInit = HashSet<ProtoNetwork<*>>()
    
    final override suspend fun run(): Boolean {
        if (node !in state)
            return false
        
        state -= node
        remove()
        state.removeNodeData(node)
        
        for (network in clustersToInit) {
            if (network !in state)
                continue
            
            network.initCluster()
        }
        
        if (updateNodes) {
            for (node in nodesToUpdate) {
                node.handleNetworkUpdate(state)
            }
        }
        
        return true
    }
    
    abstract fun remove()
    
    /**
     * Invalidates the cluster of all [ProtoNetworks][ProtoNetwork] clustered with [network] and
     * schedules them for re-initialization in via [clustersToInit].
     * Only registered networks' clusters will actually be re-initialized.
     */
    protected fun reclusterize(network: ProtoNetwork<*>) {
        val cluster = network.cluster
            ?: return
        
        for (previouslyClusteredNetwork in cluster) {
            previouslyClusteredNetwork.invalidateCluster()
            clustersToInit += previouslyClusteredNetwork
        }
    }
    
}