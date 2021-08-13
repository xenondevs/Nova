package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.EnergyUtils
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized

class EnergyBar(
    gui: GUI,
    x: Int, y: Int,
    height: Int,
    private val getEnergyValues: () -> Triple<Int, Int, Int>
) : VerticalBar(gui, x, y, height, NovaMaterial.RED_BAR) {
    
    private var energy: Int = 0
    private var maxEnergy: Int = 0
    private var energyPerTick: Int = 0
    
    init {
        update()
    }
    
    fun update() {
        val energyValues = getEnergyValues()
        energy = energyValues.first
        maxEnergy = energyValues.second
        energyPerTick = energyValues.third
        percentage = energy.toDouble() / maxEnergy.toDouble()
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.setDisplayName(EnergyUtils.getEnergyString(energy, maxEnergy))
        if (energyPerTick != -1)
            itemBuilder.addLoreLines(localized(ChatColor.GRAY, "menu.nova.energy_per_tick", EnergyUtils.getEnergyString(energyPerTick)))
        return itemBuilder
    }
    
}
