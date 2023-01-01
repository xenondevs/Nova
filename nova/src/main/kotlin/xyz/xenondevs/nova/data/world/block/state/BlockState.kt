package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.world.BlockPos

abstract class BlockState {
    
    abstract val pos: BlockPos
    abstract val id: NamespacedId
    abstract val isLoaded: Boolean
    
    internal abstract fun handleInitialized(placed: Boolean)
    
    internal abstract fun handleRemoved(broken: Boolean)
    
    internal abstract fun read(buf: ByteBuffer)
    
    internal abstract fun write(buf: ByteBuffer)
    
}