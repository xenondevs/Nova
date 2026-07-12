package xyz.xenondevs.nova.context.intention

import org.bukkit.block.Block
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.util.novaKey

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
 * | [BLOCK_STATE_NOVA] | +1. | [BLOCK] | Only if Nova block |
 * | [BLOCK_STATE] | +1. | [BLOCK] | Only if vanilla block |
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
        novaKey("include_data"),
        default = false
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalTileEntity.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(BLOCK_STATE, Autofiller.from(BLOCK, Block::getBlockData))
    }
    
}
