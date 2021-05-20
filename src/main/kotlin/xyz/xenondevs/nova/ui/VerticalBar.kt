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

open class VerticalBar(
    gui: GUI,
    x: Int, y: Int,
    height: Int,
    private val barMaterial: NovaMaterial
) {
    
    private val barItems = Array(height) { VerticalBarItem(it, height) }
    var percentage: Double = 0.0
        set(value) {
            field = value
            barItems.forEach(Item::notifyWindows)
        }
    
    init {
        ((y + height - 1) downTo y).withIndex().forEach { (index, y) -> gui.setItem(x, y, barItems[index]) }
    }
    
    protected open fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder = itemBuilder
    
    private inner class VerticalBarItem(
        private val section: Int,
        private val totalSections: Int,
    ) : BaseItem() {
        
        override fun getItemBuilder(): ItemBuilder {
            val displayPercentageStart = (1.0 / totalSections) * section
            val displayPercentage = max(min((percentage - displayPercentageStart) * totalSections, 1.0), 0.0)
            val state = round(displayPercentage * 16).toInt()
            
            return modifyItemBuilder(barMaterial.item.getItemBuilder(state))
        }
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
        
    }
    
}