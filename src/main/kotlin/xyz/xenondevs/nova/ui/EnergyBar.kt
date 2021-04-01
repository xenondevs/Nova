package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class EnergyBar(
    gui: GUI,
    x: Int, y: Int,
    height: Int,
    private val getEnergyValues: () -> Pair<Int, Int>
) {
    
    private val energyItems = Array(height) { EnergyBarItem(it, height) }
    var percentage: Double = 0.0
        set(value) {
            field = value
            energyItems.forEach(Item::notifyWindows)
        }
    
    init {
        (height downTo y).withIndex().forEach { (index, y) -> gui.setItem(x, y, energyItems[index]) }
    }
    
    private inner class EnergyBarItem(
        private val section: Int,
        private val totalSections: Int,
    ) : BaseItem() {
        
        override fun getItemBuilder(): ItemBuilder {
            val displayPercentageStart = (1.0 / totalSections) * section
            val displayPercentage = max(min((percentage - displayPercentageStart) * totalSections, 1.0), 0.0)
            val state = round(displayPercentage * 16).toInt()
            
            val energyValues = getEnergyValues()
            val energy = energyValues.first
            val maxEnergy = energyValues.second
            
            return NovaMaterial.ENERGY_BAR.item.getItemBuilder("§c$energy§8/§7$maxEnergy Energy", state)
        }
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
        
    }
    
}