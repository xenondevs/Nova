package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import kotlin.math.roundToInt

open class ProgressItem(val material: NovaMaterial, val states: Int) : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun getItemProvider(): ItemProvider {
        return material.item.createItemBuilder("", (percentage * states).roundToInt())
    }
    
    override fun handleClick(p0: ClickType?, p1: Player?, p2: InventoryClickEvent?) = Unit
}

class ProgressArrowItem : ProgressItem(NovaMaterialRegistry.PROGRESS_ARROW, 16)

class EnergyProgressItem : ProgressItem(NovaMaterialRegistry.ENERGY_PROGRESS, 16)

class PressProgressItem : ProgressItem(NovaMaterialRegistry.PRESS_PROGRESS, 8)

class PulverizerProgress : ProgressItem(NovaMaterialRegistry.PULVERIZER_PROGRESS, 14)