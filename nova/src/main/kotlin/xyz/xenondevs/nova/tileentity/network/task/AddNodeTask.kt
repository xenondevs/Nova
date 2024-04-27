package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.format.NetworkState

internal abstract class AddNodeTask<T : NetworkNode>(
    state: NetworkState,
    override val node: T,
    private val updateNodes: Boolean
) : ProtectedNodeNetworkTask(state) {
    
    protected val nodesToUpdate = HashSet<NetworkNode>()
    
    final override suspend fun run(): Boolean {
        if (node in state)
            return false
        
        state += node
        add()
        
        if (updateNodes) {
            for (node in nodesToUpdate) {
                node.handleNetworkUpdate(state)
            }
            node.handleNetworkUpdate(state)
        }
        
        return true
    }
    
    abstract fun add()
    
}