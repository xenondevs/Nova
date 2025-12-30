package xyz.xenondevs.nova.context.intention

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_ITEM_STACK
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_TYPE
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.context.intention.BlockPlace.HELD_ITEM_STACK
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.item.novaItem

/**
 * A [ContextIntention] for when a block is placed.
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasRequiredBlock], [HasOptionalTileEntity], [HasOptionalSource],
 * [HasOptionalBlockInteraction], and [HasHeldItem].
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK_ITEM_STACK] | 1. | [HELD_ITEM_STACK] | Only if block item |
 * | | 2. | [BLOCK_TYPE_NOVA] | Only if has item type |
 * | | 3. | [BLOCK_TYPE_VANILLA] | Only if has item type |
 * | [BLOCK_TYPE] | +1. | [BLOCK_ITEM_STACK] | |
 */
object BlockPlace :
    AbstractContextIntention<BlockPlace>(),
    HasRequiredBlock<BlockPlace>,
    HasOptionalTileEntity<BlockPlace>,
    HasOptionalSource<BlockPlace>,
    HasOptionalBlockInteraction<BlockPlace>,
    HasHeldItem<BlockPlace> {
    
    /**
     * The item stack to be placed as a block.
     */
    val BLOCK_ITEM_STACK = ContextParamType<ItemStack, BlockPlace>(
        Key(Nova, "block_item_stack"),
        validate = { it.type.isBlock || it.novaItem?.block != null },
        copy = ItemStack::clone
    )
    
    /**
     * Whether block place effects should be played.
     * Defaults to `true`.
     */
    val BLOCK_PLACE_EFFECTS = DefaultingContextParamType<Boolean, BlockPlace>(
        Key(Nova, "block_place_effects"),
        default = true
    )
    
    /**
     * Whether tile-entity limits should be bypassed when placing tile-entity blocks.
     * Placed blocks will still be counted.
     * Defaults to `false`.
     */
    val BYPASS_TILE_ENTITY_LIMITS = DefaultingContextParamType<Boolean, BlockPlace>(
        Key(Nova, "bypass_tile_entity_limits"),
        default = false
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalTileEntity.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(HELD_ITEM_STACK) { it.takeIf { it.type.isBlock || it.novaItem?.block != null } })
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(BLOCK_TYPE_NOVA) { it.item?.createItemStack() })
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(BLOCK_TYPE_VANILLA) { if (it.hasItemType()) it.itemType.createItemStack() else null })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_ITEM_STACK) { it.novaItem?.block?.id ?: it.type.key() })
    }
    
}