package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Event
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState

internal sealed class NetworkTask(protected val state: NetworkState) {
    
    abstract val chunkPos: ChunkPos
    
    abstract val event: Event
    
    abstract suspend fun run(): Boolean
    
}

internal sealed class ProtectedNodeNetworkTask(state: NetworkState) : NetworkTask(state) {
    
    abstract val node: NetworkNode
    override val chunkPos: ChunkPos
        get() = node.pos.chunkPos
    lateinit var result: ProtectionResult
    
}
