package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.world.BlockPos

internal class LinkedBlockState(override val pos: BlockPos, val blockState: BlockState) : BlockState {
    
    override val id = blockState.id
    override val isInitialized = true
    override fun handleInitialized(placed: Boolean) = Unit
    override fun handleRemoved(broken: Boolean) = Unit
    override fun read(buf: ByteBuffer) = throw UnsupportedOperationException()
    override fun write(buf: ByteBuffer) = throw UnsupportedOperationException()
    
    init {
        require(blockState !is LinkedBlockState) { "Linked block states cannot link to other linked block states." }
    }
    
}