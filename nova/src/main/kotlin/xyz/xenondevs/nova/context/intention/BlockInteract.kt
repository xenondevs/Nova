package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.BlockInteract.BLOCK_POS
import xyz.xenondevs.nova.context.intention.BlockInteract.BLOCK_TYPE
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.id


/**
 * A [ContextIntention] for clicking on a block.
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasRequiredBlock], [HasOptionalTileEntity], [HasOptionalSource],
 * [HasOptionalBlockInteraction], and [HasHeldItem].
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK_TYPE] | +1. | [BLOCK_POS] | |
 */
object BlockInteract :
    AbstractContextIntention<BlockInteract>(),
    HasRequiredBlock<BlockInteract>,
    HasOptionalTileEntity<BlockInteract>,
    HasOptionalSource<BlockInteract>,
    HasOptionalBlockInteraction<BlockInteract>,
    HasHeldItem<BlockInteract> {
    
    /**
     * Whether the data of the block should be included for creative-pick block interactions.
     * Defaults to `false`.
     */
    val INCLUDE_DATA = DefaultingContextParamType<Boolean, BlockInteract>(
        Key(Nova, "include_data"),
        default = false
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalTileEntity.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_POS) { it.block.id })
    }
    
}