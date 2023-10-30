package xyz.xenondevs.nova.data.world.block.property

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace

interface BlockProperty {
    
    /**
     * Initializes this [BlockProperty] when the block is being placed
     */
    fun init(ctx: Context<BlockPlace>)
    
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