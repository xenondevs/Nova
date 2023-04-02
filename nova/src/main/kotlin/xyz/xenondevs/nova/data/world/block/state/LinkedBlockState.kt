package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.world.BlockPos

internal class LinkedBlockState internal constructor(override val pos: BlockPos, val blockState: NovaBlockState) : BlockState() {
    
    override val id = blockState.id
    override val isLoaded: Boolean
        get() = blockState.isLoaded
    override fun handleInitialized(placed: Boolean) = Unit
    override fun handleRemoved(broken: Boolean) = Unit
    override fun read(buf: ByteBuffer) = throw UnsupportedOperationException()
    override fun write(buf: ByteBuffer) = throw UnsupportedOperationException()
    
}