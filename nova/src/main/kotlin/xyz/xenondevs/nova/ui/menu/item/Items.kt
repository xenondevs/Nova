package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems

internal fun clickableItem(provider: ItemProvider, run: (Player) -> Unit): Item {
    
    return object : SimpleItem(provider) {
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT) run(player)
        }
    }
    
}

val BUTTON_COLORS = listOf(
    DefaultGuiItems.RED_BTN,
    DefaultGuiItems.ORANGE_BTN,
    DefaultGuiItems.YELLOW_BTN,
    DefaultGuiItems.GREEN_BTN,
    DefaultGuiItems.BLUE_BTN,
    DefaultGuiItems.PINK_BTN,
    DefaultGuiItems.WHITE_BTN
)