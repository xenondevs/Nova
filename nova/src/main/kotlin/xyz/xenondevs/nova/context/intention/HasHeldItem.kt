@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_HAND
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_STACK
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_TYPE
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.HasHeldItem.Companion.HELD_ITEM_TYPE_VANILLA
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.item.NovaItem

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
    val HELD_ITEM_TYPE: DefaultingContextParamType<Key, I>
        get() = heldItemType()
    
    /**
     * The vanilla item type being held / used during the interaction.
     */
    val HELD_ITEM_TYPE_VANILLA: ContextParamType<ItemType, I>
        get() = heldItemTypeVanilla()
    
    /**
     * The Nova item type being held / used during the interaction.
     */
    val HELD_ITEM_TYPE_NOVA: ContextParamType<NovaItem, I>
        get() = heldItemTypeNova()
    
    /**
     * The hand in which the item is held.
     */
    val HELD_HAND: ContextParamType<EquipmentSlot, I>
        get() = heldHand()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val HELD_ITEM_STACK = DefaultingContextParamType<ItemStack, Nothing>(
            Key(Nova, "held_item_stack"),
            default = ItemStack.empty(),
            copy = ItemStack::clone
        )
        private val HELD_ITEM_TYPE = DefaultingContextParamType<Key, Nothing>(
            Key(Nova, "held_item_type"),
            default = Key.key("air")
        )
        private val HELD_ITEM_TYPE_VANILLA = ContextParamType<ItemType, Nothing>(
            Key(Nova, "held_item_type_vanilla")
        )
        private val HELD_ITEM_TYPE_NOVA = ContextParamType<NovaItem, Nothing>(
            Key(Nova, "held_item_type_nova")
        )
        private val HELD_HAND = ContextParamType<EquipmentSlot, Nothing>(
            Key(Nova, "held_hand"),
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
            HELD_ITEM_TYPE as DefaultingContextParamType<Key, I>
        
        /**
         * Gets the param type for [HELD_ITEM_TYPE_VANILLA].
         */
        fun <I : HasHeldItem<I>> heldItemTypeVanilla() =
            HELD_ITEM_TYPE_VANILLA as ContextParamType<ItemType, I>
        
        /**
         * Gets the param type for [HELD_ITEM_TYPE_NOVA].
         */
        fun <I : HasHeldItem<I>> heldItemTypeNova() =
            HELD_ITEM_TYPE_NOVA as ContextParamType<NovaItem, I>
        
        /**
         * Gets the param type for [HELD_HAND].
         */
        fun <I : HasHeldItem<I>> heldHand() =
            HELD_HAND as ContextParamType<EquipmentSlot, I>
        
        /**
         * Applies the default autofillers on [intention].
         */
        fun <I : HasHeldItem<I>> applyDefaults(intention: HasHeldItem<I>) = intention.apply {
            addAutofiller(HELD_ITEM_STACK, Autofiller.from(HELD_ITEM_TYPE, ItemUtils::getItemStack))
            addAutofiller(HELD_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY, HELD_HAND) { entity, hand -> entity.equipment?.getItem(hand) })
            addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_STACK, ItemUtils::getId))
            addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_TYPE_VANILLA, ItemType::key))
            addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_TYPE_NOVA, NovaItem::id))
            addAutofiller(HELD_ITEM_TYPE_VANILLA, Autofiller.from(HELD_ITEM_TYPE, Registry.ITEM::get))
            addAutofiller(HELD_ITEM_TYPE_NOVA, Autofiller.from(HELD_ITEM_TYPE, NovaRegistries.ITEM::getValue))
        }
        
    }
    
}