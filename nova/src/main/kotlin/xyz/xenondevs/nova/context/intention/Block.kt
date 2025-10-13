package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.AbstractContextIntention

/**
 * Abstract base class for block-related context intentions.
 */
abstract class Block<I : Block<I>> :
    AbstractContextIntention<I>(),
    HasRequiredBlock<I>,
    HasOptionalTileEntity<I>,
    HasOptionalSource<I>,
    HasOptionalBlockInteraction<I> {
    
    // HasRequiredBlock
    override val BLOCK_POS = HasRequiredBlock.blockPos<I>()
    override val BLOCK_WORLD = HasRequiredBlock.blockWorld<I>()
    override val BLOCK_TYPE = HasRequiredBlock.blockType<I>()
    override val BLOCK_TYPE_VANILLA = HasRequiredBlock.blockTypeVanilla<I>()
    override val BLOCK_TYPE_NOVA = HasRequiredBlock.blockTypeNova<I>()
    override val BLOCK_STATE_NOVA = HasRequiredBlock.blockStateNova<I>()
    
    // HasOptionalTileEntity        
    override val TILE_ENTITY_NOVA = HasOptionalTileEntity.tileEntityNova<I>()
    override val TILE_ENTITY_DATA_NOVA = HasOptionalTileEntity.tileEntityDataNova<I>()
    
    // HasOptionalSource
    override val SOURCE_UUID = HasOptionalSource.sourceUuid<I>()
    override val SOURCE_LOCATION = HasOptionalSource.sourceLocation<I>()
    override val SOURCE_WORLD = HasOptionalSource.sourceWorld<I>()
    override val SOURCE_DIRECTION = HasOptionalSource.sourceDirection<I>()
    override val SOURCE_ENTITY = HasOptionalSource.sourceEntity<I>()
    override val SOURCE_LIVING_ENTITY = HasOptionalSource.sourceLivingEntity<I>()
    override val SOURCE_PLAYER = HasOptionalSource.sourcePlayer<I>()
    override val SOURCE_TILE_ENTITY = HasOptionalSource.sourceTileEntity<I>()
    override val RESPONSIBLE_PLAYER = HasOptionalSource.responsiblePlayer<I>()
    
    // HasOptionalBlockInteraction
    override val CLICKED_BLOCK_FACE = HasOptionalBlockInteraction.clickedBlockFace<I>()
    override val INTERACTION_HAND = HasOptionalBlockInteraction.interactionHand<I>()
    override val INTERACTION_ITEM_STACK = HasOptionalBlockInteraction.interactionItemStack<I>()
    
    companion object {
        
        /**
         * Applies the default required properties and autofillers on [intention].
         */
        fun <I : Block<I>> applyDefaults(intention: I) {
            HasRequiredBlock.applyDefaults(intention)
            HasOptionalTileEntity.applyDefaults(intention)
            HasOptionalSource.applyDefaults(intention)
            HasOptionalBlockInteraction.applyDefaults(intention)
        }
        
    }
    
}