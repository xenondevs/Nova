package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.util.playClickSound

class BackItem(
    itemProvider: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.model.clientsideProvider,
    private val openPrevious: (Player) -> Unit
) : SimpleItem(itemProvider) {
    
    constructor(
        itemProvider: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.model.clientsideProvider,
        previous: Window
    ) : this(itemProvider, { previous.open() })
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        openPrevious(player)
    }
    
}