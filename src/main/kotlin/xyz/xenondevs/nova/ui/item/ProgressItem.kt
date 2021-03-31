package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial

class ProgressItem : BaseItem() {
    
    var state: Int = 0
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun getItemBuilder() = NovaMaterial.FURNACE_PROGRESS.item.getItemBuilder("", state)
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}