package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

class AnvilTextItem(val builder: ItemBuilder, text: String) : BaseItem() {
    
    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                notifyWindows()
            }
        }
    
    override fun getItemProvider(): ItemProvider {
        return builder.setDisplayName(text)
    }
    
    fun resetText() {
        text = "."
        text = ""
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}