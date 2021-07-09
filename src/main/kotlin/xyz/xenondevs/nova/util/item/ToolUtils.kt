package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.random.Random

fun Material.isShovel() = name.endsWith("_SHOVEL")

fun Material.isPickaxe() = name.endsWith("_PICKAXE")

fun Material.isAxe() = name.endsWith("_AXE")

fun Material.isHoe() = name.endsWith("_HOE")

fun Material.isSword() = name.endsWith("_SWORD")

object ToolUtils {
    
    fun damageTool(item: ItemStack): ItemStack? {
        val meta = item.itemMeta
        if (meta is Damageable) {
            if (meta.isUnbreakable)
                return item
            if (meta.hasEnchant(Enchantment.DURABILITY)) {
                val percentage = 100.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1)
                if (Random.nextInt(0, 100) >= percentage)
                    return item
            }
            meta.damage += 1
            if (meta.damage >= item.type.maxDurability)
                return null
            item.itemMeta = meta
        }
        return item
    }
    
}
