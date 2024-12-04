package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.AbstractScrollGuiBoundItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls up one line on left-click.
 */
class ScrollUpItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_UP_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_UP_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.canScroll(-1)) {
            player.playClickSound()
            gui.scroll(-1)
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls down one line on left-click.
 */
class ScrollDownItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_DOWN_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_DOWN_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.canScroll(1)) {
            player.playClickSound()
            gui.scroll(1)
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls left one column on left-click.
 */
class ScrollLeftItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.canScroll(-1)) {
            player.playClickSound()
            gui.scroll(-1)
        }
    }
    
}

/**
 * A UI item for [ScrollGuis][ScrollGui] that scrolls right one column on left-click.
 */
class ScrollRightItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider
) : AbstractScrollGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.canScroll(1)) {
            player.playClickSound()
            gui.scroll(1)
        }
    }
    
}