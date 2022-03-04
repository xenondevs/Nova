package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.clientsideDurability
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.retrieveData
import xyz.xenondevs.nova.util.storeData

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
        itemStack.itemMeta = itemMeta
        
        itemStack.clientsideDurability = energy.toDouble() / maxEnergy.toDouble()
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        setEnergy(itemStack, getEnergy(itemStack) + energy)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it, 0); it }
        return itemBuilder
    }
    
}