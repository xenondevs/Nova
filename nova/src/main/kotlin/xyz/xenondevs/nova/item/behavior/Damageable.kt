package xyz.xenondevs.nova.item.behavior

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.item.ItemDisplayData
import xyz.xenondevs.nova.util.item.retrieveDataOrNull
import xyz.xenondevs.nova.util.item.storeData
import kotlin.math.min

private val DAMAGE_KEY = NamespacedKey(NOVA, "item_damage")

class Damageable(
    maxDurability: ValueReloadable<Int>
) : ItemBehavior() {
    
    val maxDurability by maxDurability
    
    fun getDamage(itemStack: ItemStack): Int {
        return min(maxDurability, itemStack.retrieveDataOrNull(DAMAGE_KEY) ?: 0)
    }
    
    fun setDamage(itemStack: ItemStack, damage: Int) {
        val coercedDamage = damage.coerceIn(0..maxDurability)
        itemStack.storeData(DAMAGE_KEY, coercedDamage)
    }
    
    fun addDamage(itemStack: ItemStack, damage: Int) {
        setDamage(itemStack, getDamage(itemStack) + damage)
    }
    
    fun getDurability(itemStack: ItemStack): Int {
        return maxDurability - getDamage(itemStack)
    }
    
    fun setDurability(itemStack: ItemStack, durability: Int) {
        setDamage(itemStack, maxDurability - durability)
    }
    
    override fun updateItemDisplay(itemStack: ItemStack, display: ItemDisplayData) {
        val damage = getDamage(itemStack)
        display.durability = damage.toDouble() / maxDurability.toDouble()
    }
    
}