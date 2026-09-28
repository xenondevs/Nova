package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.ReferencingInventory

// TODO: Move shared utilities into a dedicated utility module.

/**
 * Adds an [ItemStack] to an [Inventory] while respecting both
 * the max stack size of the inventory and the max stack size
 * of the item type.
 *
 * Unlike Bukkit's addItem method, the [ItemStack] provided as the
 * method parameter will not be modified.
 *
 * @return The amount of items that did not fit.
 */
fun Inventory.addItemCorrectly(itemStack: ItemStack): Int =
    ReferencingInventory.fromStorageContents(this).addItem(null, itemStack)

/**
 * Adds [items] to the [Player's][Player] inventory or drops them on
 * the ground if there is not enough space.
 */
fun Player.addToInventoryOrDrop(vararg items: ItemStack) =
    addToInventoryOrDrop(items.asList())

/**
 * Adds [items] to the [Player's][Player] inventory or drops them on
 * the ground if there is not enough space.
 */
fun Player.addToInventoryOrDrop(items: List<ItemStack>) {
    val inventory = inventory
    items.forEach {
        val leftover = inventory.addItemCorrectly(it)
        if (leftover > 0) {
            val drop = it.clone().apply { amount = leftover }
            dropItemLikePlayer(this, drop)
        }
    }
}

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory]
 * if the given slot is occupied or drops it on the ground if there is not enough space.
 */
fun Player.addToInventoryPrioritizedOrDrop(prioritizedSlot: Int, itemStack: ItemStack) {
    val inventory = inventory
    if (inventory.getItem(prioritizedSlot)?.isEmpty != false) {
        inventory.setItem(prioritizedSlot, itemStack)
    } else {
        addToInventoryOrDrop(itemStack)
    }
}

/**
 * Puts an [ItemStack] on the [prioritizedSlot] or adds it to the [Inventory]
 * if the given slot is occupied or drops it on the ground if there is not enough space.
 */
fun LivingEntity.addToInventoryPrioritizedOrDrop(prioritizedSlot: EquipmentSlot, itemStack: ItemStack) {
    val inventory = (this as? InventoryHolder)?.inventory
    val equipment = equipment
    if (equipment != null && equipment.getItem(prioritizedSlot).isEmpty) {
        equipment.setItem(prioritizedSlot, itemStack)
    } else {
        val leftover = inventory?.addItemCorrectly(itemStack) ?: itemStack.amount
        if (leftover > 0) {
            val drop = itemStack.clone().apply { amount = leftover }
            dropItemLikePlayer(this, drop)
        }
    }
}

private fun dropItemLikePlayer(entity: LivingEntity, itemStack: ItemStack): Boolean {
    if (itemStack.isEmpty)
        return true
    
    val location: Location
    if (entity is Player) {
        location = entity.location
        location.add(0.0, 1.5, 0.0)
    } else {
        location = entity.eyeLocation
    }
    
    val item = location.world.createEntity(location, Item::class.java)
    item.itemStack = itemStack.clone()
    item.pickupDelay = 40
    item.velocity = location.direction.multiply(0.35)
    
    if (entity !is Player || PlayerDropItemEvent(entity, item).callEvent()) {
        location.world.addEntity(item)
        return true
    }
    
    return false
}
