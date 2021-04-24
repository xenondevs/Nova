package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import kotlin.math.roundToInt

open class ProgressItem(val material: NovaMaterial, val states: Int) : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun getItemBuilder(): ItemBuilder {
        return material.item.getItemBuilder("", (percentage * states).roundToInt())
    }
    
    override fun handleClick(p0: ClickType?, p1: Player?, p2: InventoryClickEvent?) = Unit
}

class ProgressArrowItem : ProgressItem(NovaMaterial.PROGRESS_ARROW, 16)

class EnergyProgressItem : ProgressItem(NovaMaterial.ENERGY_PROGRESS, 16)

class PressProgressItem : ProgressItem(NovaMaterial.PRESS_PROGRESS, 8)

class PulverizerProgress : ProgressItem(NovaMaterial.PULVERIZER_PROGRESS, 14)