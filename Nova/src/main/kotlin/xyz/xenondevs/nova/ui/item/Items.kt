package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.SimpleItem
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