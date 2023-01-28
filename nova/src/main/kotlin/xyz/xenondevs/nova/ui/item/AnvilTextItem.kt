package xyz.xenondevs.nova.ui.item

import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.impl.BaseItem
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
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
    
}