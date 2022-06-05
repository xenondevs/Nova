package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.material.clientsideDurability
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData

private val ENERGY_KEY = NamespacedKey(NOVA, "item_energy")

class Chargeable(
    maxEnergy: ValueReloadable<Long>,
) : ItemBehavior() {
    
    val maxEnergy by maxEnergy
    
    fun getEnergy(itemStack: ItemStack) : Long {
        val currentEnergy = itemStack.retrieveData(ENERGY_KEY) ?: 0L
        if (currentEnergy > maxEnergy) {
            setEnergy(itemStack, maxEnergy)
            return maxEnergy
        }
        return currentEnergy
    }
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        val coercedEnergy = energy.coerceIn(0, maxEnergy)
        itemStack.storeData(ENERGY_KEY, coercedEnergy)
        itemStack.clientsideDurability = coercedEnergy.toDouble() / maxEnergy.toDouble()
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        setEnergy(itemStack, getEnergy(itemStack) + energy)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it, 0); it }
        return itemBuilder
    }
    
    override fun getLore(itemStack: ItemStack): List<Array<BaseComponent>> {
        val energy = getEnergy(itemStack)
        return listOf(TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy, maxEnergy)))
    }
    
}