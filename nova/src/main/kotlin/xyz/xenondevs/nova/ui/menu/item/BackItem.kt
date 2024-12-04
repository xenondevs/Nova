package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems

class BackItem(
    private val itemProvider: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val openPrevious: (Player) -> Unit
) : AbstractItem() {
    
    constructor(
        itemProvider: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
        previous: Window
    ) : this(itemProvider, { previous.open() })
    
    override fun getItemProvider(player: Player) = itemProvider
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        player.playClickSound()
        openPrevious(player)
    }
    
}