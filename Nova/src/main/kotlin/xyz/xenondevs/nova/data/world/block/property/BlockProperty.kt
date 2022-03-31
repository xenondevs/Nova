package xyz.xenondevs.nova.data.world.block.property

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

interface BlockProperty {
    
    /**
     * Initializes this [BlockProperty] when the block is being placed
     */
    fun init(ctx: BlockPlaceContext)
    
    /**
     * Reads the values of this [BlockProperty] from the given [ByteBuf]
     */
    fun read(buf: ByteBuf)
    
    /**
     * Write the values of this [BlockProperty] to the given [ByteBuf]
     */
    fun write(buf: ByteBuf)
    
}

interface BlockPropertyType<T : BlockProperty> {
    
    fun create() : T
    
}