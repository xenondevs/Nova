package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.addToInventoryPrioritizedOrDrop
import xyz.xenondevs.nova.util.damageItemInHand
import xyz.xenondevs.nova.util.item.damage
import xyz.xenondevs.nova.util.item.takeUnlessEmpty

private val ItemStack.useRemainder: ItemStack?
    get() = getData(DataComponentTypes.USE_REMAINDER)?.transformInto()

/**
 * An action to be applied to an item after an interaction.
 */
interface ItemAction {
    
    /**
     * Applies this action to the item in the [slot] of [entity].
     */
    fun apply(entity: LivingEntity, slot: EquipmentSlot)
    
    /**
     * Applies this action to a copy of [itemStack], in the context of [world], then returns it.
     * Does not modify the original [itemStack].
     */
    fun apply(world: World, itemStack: ItemStack): List<ItemStack>
    
    /**
     * The stack is shrunk by [decrement] and a corresponding amount of use remainder items is added, if there is one.
     */
    class Consume(
        val decrement: Int = 1
    ) : ItemAction {
        
        override fun apply(entity: LivingEntity, slot: EquipmentSlot) {
            if (entity is HumanEntity && entity.gameMode == GameMode.CREATIVE)
                return
            
            val equipment = entity.equipment ?: return
            val useRemainder = equipment.getItem(slot).useRemainder?.apply { amount = decrement }
            
            equipment.getItem(slot).amount -= decrement
            if (useRemainder != null)
                entity.addToInventoryPrioritizedOrDrop(slot, useRemainder)
        }
        
        override fun apply(world: World, itemStack: ItemStack): List<ItemStack> {
            return listOfNotNull(
                itemStack.clone().also { it.amount -= decrement }.takeUnlessEmpty(),
                itemStack.useRemainder?.apply { amount = decrement }
            )
        }
        
    }
    
    /**
     * The item is damaged by [damage].
     */
    class Damage(
        /**
         * The amount of damage to apply to the item.
         */
        val damage: Int = 1
    ) : ItemAction {
        
        override fun apply(entity: LivingEntity, slot: EquipmentSlot) {
            if (entity is HumanEntity && entity.gameMode == GameMode.CREATIVE)
                return
            val useRemainder = entity.equipment?.getItem(slot)?.useRemainder
            if (entity.damageItemInHand(slot, damage) && useRemainder != null) {
                entity.addToInventoryPrioritizedOrDrop(slot, useRemainder)
            }
        }
        
        override fun apply(world: World, itemStack: ItemStack): List<ItemStack> {
            val result = itemStack.clone().damage(damage, world)
            return listOfNotNull(result ?: itemStack.useRemainder)
        }
        
    }
    
    /**
     * One item from the stack is converted into [newItemStack].
     * [newItemStack] will not be modified.
     */
    class ConvertOne(newItemStack: ItemStack) : ItemAction {
        
        /**
         * The new item stack that one item is converted into.
         */
        val newItemStack = newItemStack.clone()
        
        override fun apply(entity: LivingEntity, slot: EquipmentSlot) {
            if (entity is Player && entity.gameMode != GameMode.CREATIVE)
                entity.inventory.getItem(slot).amount--
            entity.addToInventoryPrioritizedOrDrop(slot, newItemStack.clone())
        }
        
        override fun apply(world: World, itemStack: ItemStack): List<ItemStack> {
            if (itemStack.amount > 1) {
                val remaining = itemStack.clone().apply { amount-- }
                return listOf(remaining, newItemStack.clone())
            } else {
                return listOf(newItemStack.clone())
            }
        }
        
    }
    
    /**
     * The entire stack is converted into [newItemStack].
     * [newItemStack] will not be modified.
     */
    class ConvertStack(newItemStack: ItemStack) : ItemAction {
        
        /**
         * The new item stack that the entire stack is converted into.
         */
        val newItemStack = newItemStack.clone()
        
        override fun apply(entity: LivingEntity, slot: EquipmentSlot) {
            entity.equipment?.setItem(slot, newItemStack.clone())
        }
        
        override fun apply(world: World, itemStack: ItemStack): List<ItemStack> =
            listOf(newItemStack.clone())
        
    }
    
    /**
     * No action is applied.
     */
    data object None : ItemAction {
        override fun apply(entity: LivingEntity, slot: EquipmentSlot) = Unit
        override fun apply(world: World, itemStack: ItemStack) = listOf(itemStack)
    }
    
}