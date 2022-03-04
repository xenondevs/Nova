package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.retrieveData
import xyz.xenondevs.nova.util.storeData
import kotlin.math.roundToInt

private val ENERGY_KEY = NamespacedKey(NOVA, "item_energy64")

class Chargeable(
    val maxEnergy: Long,
) : ItemBehavior() {
    
    fun getEnergy(itemStack: ItemStack) = itemStack.retrieveData(ENERGY_KEY) ?: 0L
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        val coercedEnergy = energy.coerceIn(0, maxEnergy)
        
        itemStack.storeData(ENERGY_KEY, coercedEnergy)
        
        val itemMeta = itemStack.itemMeta!!
        itemMeta.lore = listOf("§7" + NumberFormatUtils.getEnergyString(coercedEnergy, maxEnergy))
        if (itemMeta is Damageable)
            itemMeta.damage = calculateDamage(itemStack.type, coercedEnergy)
        itemStack.itemMeta = itemMeta
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        setEnergy(itemStack, getEnergy(itemStack) + energy)
    }
    
    private fun calculateDamage(material: Material, energy: Long): Int {
        val percentage = energy.toDouble() / maxEnergy.toDouble()
        val maxDurability = material.maxDurability
        return (maxDurability - (maxDurability * percentage)).roundToInt()
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it, 0); it }
        return itemBuilder
    }
    
}