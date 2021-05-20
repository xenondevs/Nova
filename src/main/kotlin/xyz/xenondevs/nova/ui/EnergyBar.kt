package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemBuilder
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.EnergyUtils

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
        itemBuilder.displayName = EnergyUtils.getEnergyString(energy, maxEnergy)
        if (energyPerTick != -1)
            itemBuilder.addLoreLines("§7" + EnergyUtils.getEnergyString(energyPerTick) + " / tick")
        return itemBuilder
    }
    
}
