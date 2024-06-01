package xyz.xenondevs.nova.tileentity.network.task

import jdk.jfr.Event
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.format.NetworkState

internal sealed class NetworkTask(protected val state: NetworkState) {
 
    abstract val event: Event
    
    abstract suspend fun run(): Boolean
    
}

internal sealed class ProtectedNodeNetworkTask(state: NetworkState) : NetworkTask(state) {
    
    abstract val node: NetworkNode
    lateinit var result: ProtectionResult
    
}
