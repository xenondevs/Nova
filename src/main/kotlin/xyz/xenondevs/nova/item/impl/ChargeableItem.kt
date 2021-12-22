package xyz.xenondevs.nova.item.impl

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.player.equipment.EquipMethod
import xyz.xenondevs.nova.util.PrefixUtils
import kotlin.math.roundToInt

private val ENERGY_KEY = NamespacedKey(NOVA, "item_energy64")

abstract class ChargeableItem(
    val maxEnergy: Long,
) : NovaItem() {
    
    fun getEnergy(itemStack: ItemStack) = retrieveData(itemStack, ENERGY_KEY) ?: 0L
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        val coercedEnergy = energy.coerceIn(0, maxEnergy)
        
        storeData(itemStack, ENERGY_KEY, coercedEnergy)
        
        val itemMeta = itemStack.itemMeta!!
        itemMeta.lore = listOf(PrefixUtils.getEnergyString(coercedEnergy, maxEnergy))
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
    
    override fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        if (event.equipMethod == EquipMethod.BREAK) {
            event.isCancelled = true
        }
    }
    
    override fun getDefaultItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.damage = calculateDamage(itemBuilder.material, 0)
        itemBuilder.addLoreLines(PrefixUtils.getEnergyString(0, maxEnergy))
        return itemBuilder
    }
    
}