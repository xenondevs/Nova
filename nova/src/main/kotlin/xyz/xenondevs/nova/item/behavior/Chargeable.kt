package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.item.retrieveDataOrNull
import xyz.xenondevs.nova.util.item.storeData

private val ENERGY_KEY = NamespacedKey(NOVA, "item_energy")

class Chargeable(
    maxEnergy: ValueReloadable<Long>,
    private val affectsItemDurability: Boolean = true
) : ItemBehavior() {
    
    override val vanillaMaterialProperties = if (affectsItemDurability)
        listOf(VanillaMaterialProperty.DAMAGEABLE)
    else emptyList()
    
    val maxEnergy by maxEnergy
    
    fun getEnergy(itemStack: ItemStack): Long {
        val currentEnergy = itemStack.retrieveDataOrNull(ENERGY_KEY) ?: 0L
        if (currentEnergy > maxEnergy) {
            setEnergy(itemStack, maxEnergy)
            return maxEnergy
        }
        return currentEnergy
    }
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        val coercedEnergy = energy.coerceIn(0, maxEnergy)
        itemStack.storeData(ENERGY_KEY, coercedEnergy)
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        setEnergy(itemStack, getEnergy(itemStack) + energy)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it, 0); it }
        return itemBuilder
    }
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        val energy = getEnergy(itemStack)
        
        itemData.addLore(TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy, maxEnergy)))
        
        if (affectsItemDurability)
            itemData.durabilityBar = energy.toDouble() / maxEnergy.toDouble()
    }
    
}