package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import kotlin.math.roundToInt

class EnergyProgressItem : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun getItemBuilder() = NovaMaterial.ENERGY_PROGRESS.item.getItemBuilder("", (percentage * 16.0).roundToInt())
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}