package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_ITEM_STACK
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.BlockPlace.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.context.intention.BlockPlace.INTERACTION_ITEM_STACK
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.item.novaItem

/**
 * Context intention for when a block is placed.
 */
object BlockPlace : Block<BlockPlace>() {
    
    /**
     * The block type as id.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * - [BLOCK_TYPE_VANILLA]
     * - [BLOCK_ITEM_STACK]
     */
    override val BLOCK_TYPE: RequiredContextParamType<Key, BlockPlace> =
        HasRequiredBlock.blockType()
    
    /**
     * The item stack to be placed as a block.
     *
     * Autofilled by:
     * - [INTERACTION_ITEM_STACK] if block item
     * - [BLOCK_TYPE_NOVA] if the block has an item type
     * - [BLOCK_TYPE_VANILLA] if the block has an item type
     */
    val BLOCK_ITEM_STACK = ContextParamType<ItemStack, BlockPlace>(
        Key(Nova, "block_item_stack"),
        validate = { it.type.isBlock || it.novaItem?.block != null },
        copy = ItemStack::clone
    )
    
    /**
     * Whether block place effects should be played.
     * Defaults to `true`.
     *
     * Autofilled by: none
     */
    val BLOCK_PLACE_EFFECTS = DefaultingContextParamType<Boolean, BlockPlace>(
        Key(Nova, "block_place_effects"),
        default = true
    )
    
    /**
     * Whether tile-entity limits should be bypassed when placing tile-entity blocks.
     * Placed blocks will still be counted.
     * Defaults to `false`.
     *
     * Autofilled by: none
     */
    val BYPASS_TILE_ENTITY_LIMITS= DefaultingContextParamType<Boolean, BlockPlace>(
        Key(Nova, "bypass_tile_entity_limits"),
        default = false
    )
    
    init {
        applyDefaults(this)
        
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(INTERACTION_ITEM_STACK) { it.takeIf { it.type.isBlock || it.novaItem?.block != null } })
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(BLOCK_TYPE_NOVA) { it.item?.createItemStack() })
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(BLOCK_TYPE_VANILLA) { if (it.hasItemType()) it.itemType.createItemStack() else null })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_ITEM_STACK) { it.novaItem?.block?.id ?: it.type.key() })
    }
    
}