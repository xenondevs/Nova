package xyz.xenondevs.nova.player.equipment

import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot

enum class ArmorType(val equipmentSlot: BukkitEquipmentSlot) {
    
    HELMET(BukkitEquipmentSlot.HEAD),
    CHESTPLATE(BukkitEquipmentSlot.CHEST),
    LEGGINGS(BukkitEquipmentSlot.LEGS),
    BOOTS(BukkitEquipmentSlot.FEET);
    
}