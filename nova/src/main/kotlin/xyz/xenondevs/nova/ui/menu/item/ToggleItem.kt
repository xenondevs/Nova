package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.AbstractItem

internal class ToggleItem(
    var state: Boolean,
    val on: ItemProvider,
    val off: ItemProvider,
    val onToggle: (Boolean) -> Boolean
) : AbstractItem() {
    
    override fun getItemProvider(): ItemProvider {
        return if (state) on else off
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (onToggle(!state)) {
            state = !state
            notifyWindows()
        }
    }
}