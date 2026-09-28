package xyz.xenondevs.nova.util

import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot

// TODO: Move shared utilities into a dedicated utility module.

/**
 * Damages the item in the specified [hand] by [damage] amount
 * as if the entity caused it and returns whether the item broke.
 */
fun LivingEntity.damageItemInHand(hand: EquipmentSlot, damage: Int = 1): Boolean {
    if (damage <= 0)
        return false
    
    val nmsEntity = (this as CraftLivingEntity).handle
    val itemInHand = nmsEntity.getItemInHand(CraftEquipmentSlot.getHand(hand))
    var broken = false
    itemInHand.hurtAndBreak(
        damage,
        (world as CraftWorld).handle,
        nmsEntity,
        {
            nmsEntity.onEquippedItemBroken(it, CraftEquipmentSlot.getNMS(hand))
            broken = true
        },
        true
    )
    
    return broken
}
