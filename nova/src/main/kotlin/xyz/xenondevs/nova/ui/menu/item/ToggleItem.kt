package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemProvider

internal class ToggleItem(
    var state: Boolean,
    val on: ItemProvider,
    val off: ItemProvider,
    val onToggle: (Boolean) -> Boolean
) : AbstractItem() {
    
    override fun getItemProvider(player: Player): ItemProvider {
        return if (state) on else off
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (onToggle(!state)) {
            state = !state
            notifyWindows()
        }
    }
}