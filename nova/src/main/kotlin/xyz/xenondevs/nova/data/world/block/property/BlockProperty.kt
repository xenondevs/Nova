package xyz.xenondevs.nova.data.world.block.property

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

interface BlockProperty {
    
    /**
     * Initializes this [BlockProperty] when the block is being placed
     */
    fun init(ctx: BlockPlaceContext)
    
    /**
     * Reads the values of this [BlockProperty] from the given [Compound]
     */
    fun read(compound: Compound)
    
    /**
     * Writes the values of this [BlockProperty] to the given [Compound]
     */
    fun write(compound: Compound)
    
}

interface BlockPropertyType<T : BlockProperty> {
    
    fun create() : T
    
}