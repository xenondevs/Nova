package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.world.BlockPos

sealed interface BlockState {
    
    val pos: BlockPos
    val id: NamespacedId
    val isInitialized: Boolean
    
    fun handleInitialized(placed: Boolean)
    
    fun handleRemoved(broken: Boolean)
    
    fun read(buf: ByteBuffer)
    
    fun write(buf: ByteBuffer)
    
}