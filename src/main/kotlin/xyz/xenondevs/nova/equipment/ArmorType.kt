package xyz.xenondevs.nova.equipment

import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

enum class ArmorType(val equipmentSlot: EquipmentSlot, val rawSlot: Int, nameSuffix: String) {
    
    HELMET(EquipmentSlot.HEAD, 5, "HELMET"),
    CHESTPLATE(EquipmentSlot.CHEST, 6, "CHESTPLATE"),
    LEGGINGS(EquipmentSlot.LEGS, 7, "LEGGINGS"),
    BOOTS(EquipmentSlot.FEET, 8, "BOOTS");
    
    val materials = Material.values().filterTo(HashSet()) { it.name.endsWith(nameSuffix) }
    
    companion object {
        
        val ARMOR_EQUIPMENT_SLOTS = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        
        fun of(itemStack: ItemStack?): ArmorType? {
            if (itemStack == null) return null
            
            val material = itemStack.type
            return values().firstOrNull { it.materials.contains(material) }
        }
        
        fun of(rawSlot: Int) = values().firstOrNull { it.rawSlot == rawSlot }
        
    }
    
}