package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.world.BlockPos

sealed interface BlockState {
    
    val pos: BlockPos
    val id: NamespacedId
    
    fun handleInitialized(placed: Boolean)
    
    fun handleRemoved(broken: Boolean)
    
    fun read(buf: ByteBuf)
    
    fun write(buf: ByteBuf)
    
}