package xyz.xenondevs.nova.ui.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.util.playClickSound

class BackItem(private val openPrevious: (Player) -> Unit) : SimpleItem(DefaultGuiItems.ARROW_LEFT_ON.model.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        openPrevious(player)
    }
    
}