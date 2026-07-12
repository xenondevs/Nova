@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_HAND
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_STACK
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_TYPE
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.item.itemType

/**
 * A [ContextIntention] that has parameters about an item being held during an interaction.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [HELD_ITEM_STACK] | 1. | [HELD_ITEM_TYPE] | Default item stack |
 * | | 2. | [SOURCE_LIVING_ENTITY], [HELD_HAND] | |
 * | [HELD_ITEM_TYPE] | 1. | [HELD_ITEM_STACK] | |
 * | | 2. | [HELD_ITEM_TYPE_VANILLA] | |
 * | | 3. | [HELD_ITEM_TYPE_NOVA] | |
 * | [HELD_ITEM_TYPE_VANILLA] | 1. | [HELD_ITEM_TYPE] | Only if vanilla item |
 * | [HELD_ITEM_TYPE_NOVA] | 1. | [HELD_ITEM_TYPE] | Only if Nova item |
 */
interface HasHeldItem<I : HasHeldItem<I>> : HasOptionalSource<I> {
    
    /**
     * The item stack being held / used during the interaction.
     * Defaults to an empty item stack.
     */
    val HELD_ITEM_STACK: DefaultingContextParamType<ItemStack, I>
        get() = heldItemStack()
    
    /**
     * The item type being held / used during the interaction as id.
     * Implicitly defaults to `minecraft:air`
     */
    val HELD_ITEM_TYPE: DefaultingContextParamType<ItemType, I>
        get() = heldItemType()
    
    /**
     * The hand in which the item is held.
     */
    val HELD_HAND: ContextParamType<EquipmentSlot, I>
        get() = heldHand()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val HELD_ITEM_STACK = DefaultingContextParamType<ItemStack, Nothing>(
            novaKey("held_item_stack"),
            default = ItemStack.empty(),
            copy = ItemStack::clone
        )
        private val HELD_ITEM_TYPE = DefaultingContextParamType<ItemType, Nothing>(
            novaKey("held_item_type"),
            default = ItemType.AIR
        )
        private val HELD_HAND = ContextParamType<EquipmentSlot, Nothing>(
            novaKey("held_hand"),
            validate = EquipmentSlot::isHand
        )
        
        /**
         * Gets the param type for [HELD_ITEM_STACK].
         */
        fun <I : HasHeldItem<I>> heldItemStack() =
            HELD_ITEM_STACK as DefaultingContextParamType<ItemStack, I>
        
        /**
         * Gets the param type for [HELD_ITEM_TYPE].
         */
        fun <I : HasHeldItem<I>> heldItemType() =
            HELD_ITEM_TYPE as DefaultingContextParamType<ItemType, I>
        
        /**
         * Gets the param type for [HELD_HAND].
         */
        fun <I : HasHeldItem<I>> heldHand() =
            HELD_HAND as ContextParamType<EquipmentSlot, I>
        
        /**
         * Applies the default autofillers on [intention].
         */
        fun <I : HasHeldItem<I>> applyDefaults(intention: HasHeldItem<I>) = intention.apply {
            addAutofiller(HELD_ITEM_STACK, Autofiller.from(HELD_ITEM_TYPE, ItemType::createItemStack))
            addAutofiller(HELD_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY, HELD_HAND) { entity, hand -> entity.equipment?.getItem(hand) })
            addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_STACK, ItemStack::itemType))
        }
        
    }
    
}