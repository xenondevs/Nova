package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.EnergyUtils
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized

class EnergyBar(
    gui: GUI,
    x: Int, y: Int,
    height: Int,
    private val energyHolder: EnergyHolder
) : VerticalBar(gui, x, y, height, NovaMaterialRegistry.RED_BAR) {
    
    private var energy: Int = 0
    private var maxEnergy: Int = 0
    private val energyPlusPerTick: Int
        get() = energyHolder.energyPlus
    private val energyMinusPerTick: Int
        get() = energyHolder.energyMinus
    
    init {
        energyHolder.updateHandlers += ::update
        update()
    }
    
    fun update() {
        energy = energyHolder.energy
        maxEnergy = energyHolder.maxEnergy
        percentage = energy.toDouble() / maxEnergy.toDouble()
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.setDisplayName(EnergyUtils.getEnergyString(energy, maxEnergy))
        if (energyPlusPerTick > 0) {
            itemBuilder.addLoreLines(localized(
                ChatColor.GRAY,
                "menu.nova.energy_per_tick",
                "+" + EnergyUtils.getEnergyString(energyPlusPerTick)
            ))
        }
        if (energyMinusPerTick > 0) {
            itemBuilder.addLoreLines(localized(
                ChatColor.GRAY,
                "menu.nova.energy_per_tick",
                "-" + EnergyUtils.getEnergyString(energyMinusPerTick)
            ))
        }
        return itemBuilder
    }
    
}
