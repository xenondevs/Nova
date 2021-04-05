package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import kotlin.math.roundToInt

class PressProgressItem : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun getItemBuilder() = NovaMaterial.PRESS_PROGRESS.item.getItemBuilder("", (percentage * 8.0).roundToInt())
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}