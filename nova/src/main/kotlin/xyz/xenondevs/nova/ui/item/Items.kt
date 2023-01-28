package xyz.xenondevs.nova.ui.item

import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.SimpleItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

fun clickableItem(provider: ItemProvider, run: (Player) -> Unit): Item {
    
    return object : SimpleItem(provider) {
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT) run(player)
        }
    }
    
}