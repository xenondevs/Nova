package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.world.BlockPos

internal class LinkedBlockState(override val pos: BlockPos, val blockState: BlockState) : BlockState {
    
    init {
        require(blockState !is LinkedBlockState) { "Linked block states cannot link to other linked block states." }
    }
    
    override val id = blockState.id
    override fun handleInitialized(placed: Boolean) = Unit
    override fun handleRemoved(broken: Boolean) = Unit
    override fun read(buf: ByteBuf) = throw UnsupportedOperationException()
    override fun write(buf: ByteBuf) = throw UnsupportedOperationException()
}