package xyz.xenondevs.nova.item.behavior

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.item.retrieveDataOrNull
import xyz.xenondevs.nova.util.item.storeData
import kotlin.math.min

private val DAMAGE_KEY = NamespacedKey(NOVA, "damage")

class Damageable(val maxDurability: Int) : ItemBehavior() {
    
    override val vanillaMaterialProperties = listOf(VanillaMaterialProperty.DAMAGEABLE)
    
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
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        val damage = getDamage(itemStack)
        itemData.damage = damage
        itemData.maxDurability = maxDurability
    }
    
}