package xyz.xenondevs.nova.data.world.block.state

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.world.BlockPos

abstract class BlockState {
    
    abstract val pos: BlockPos
    abstract val id: ResourceLocation
    abstract val isLoaded: Boolean
    
    internal abstract fun handleInitialized(placed: Boolean)
    
    internal abstract fun handleRemoved(broken: Boolean)
    
    internal abstract fun read(buf: ByteBuffer)
    
    internal abstract fun write(buf: ByteBuffer)
    
}