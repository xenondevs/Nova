package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode

internal sealed class NetworkTask(protected val state: NetworkState) {
 
    abstract suspend fun run(): Boolean
    
}

internal sealed class ProtectedNodeNetworkTask(state: NetworkState) : NetworkTask(state) {
    
    abstract val node: NetworkNode
    lateinit var result: ProtectionResult
    
}
