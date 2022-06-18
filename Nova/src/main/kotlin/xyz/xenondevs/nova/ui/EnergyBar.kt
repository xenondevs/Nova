package xyz.xenondevs.nova.ui

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized

class EnergyBar(
    height: Int,
    private val energyHolder: NovaEnergyHolder
) : VerticalBar(height) {
    
    override val barMaterial = CoreGUIMaterial.BAR_RED
    
    private var energy: Long = 0
    private var maxEnergy: Long = 0
    private val energyPlusPerTick: Long
        get() = energyHolder.energyPlus
    private val energyMinusPerTick: Long
        get() = energyHolder.energyMinus
    
    init {
        energyHolder.updateHandlers += ::update
        update()
    }
    
    fun update() {
        energy = energyHolder.energy
        maxEnergy = energyHolder.maxEnergy
        percentage = (energy.toDouble() / maxEnergy.toDouble()).coerceIn(0.0, 1.0)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        if (energy == Long.MAX_VALUE) itemBuilder.setDisplayName("∞ J / ∞ J")
        else itemBuilder.setDisplayName(NumberFormatUtils.getEnergyString(energy, maxEnergy))
        
        if (energyPlusPerTick > 0) {
            itemBuilder.addLoreLines(localized(
                ChatColor.GRAY,
                "menu.nova.energy_per_tick",
                "+" + NumberFormatUtils.getEnergyString(energyPlusPerTick)
            ))
        }
        if (energyMinusPerTick > 0) {
            itemBuilder.addLoreLines(localized(
                ChatColor.GRAY,
                "menu.nova.energy_per_tick",
                "-" + NumberFormatUtils.getEnergyString(energyMinusPerTick)
            ))
        }
        return itemBuilder
    }
    
}
