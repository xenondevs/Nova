package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider

internal class AnvilTextItem(val builder: ItemBuilder, text: String) : AbstractItem() {
    
    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                notifyWindows()
            }
        }
    
    override fun getItemProvider(player: Player): ItemProvider {
        return builder.setName(text)
    }
    
    fun resetText() {
        text = "."
        text = ""
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) = Unit
    
}