package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems

/**
 * An ui [ControlItem] for [ScrollGuis][ScrollGui] that scrolls up one line on left-click.
 */
class ScrollUpItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_UP_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_UP_OFF.model.clientsideProvider
) : ControlItem<ScrollGui<*>>() {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.canScroll(-1)) {
            player.playClickSound()
            gui.scroll(-1)
        }
    }
    
}

/**
 * An ui [ControlItem] for [ScrollGuis][ScrollGui] that scrolls down one line on left-click.
 */
class ScrollDownItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_DOWN_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_DOWN_OFF.model.clientsideProvider
) : ControlItem<ScrollGui<*>>() {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.canScroll(1)) {
            player.playClickSound()
            gui.scroll(1)
        }
    }
    
}

/**
 * An ui [ControlItem] for [ScrollGuis][ScrollGui] that scrolls left one column on left-click.
 */
class ScrollLeftItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_LEFT_OFF.model.clientsideProvider
) : ControlItem<ScrollGui<*>>() {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.canScroll(-1)) {
            player.playClickSound()
            gui.scroll(-1)
        }
    }
    
}

/**
 * An ui [ControlItem] for [ScrollGuis][ScrollGui] that scrolls right one column on left-click.
 */
class ScrollRightItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_RIGHT_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_RIGHT_OFF.model.clientsideProvider
) : ControlItem<ScrollGui<*>>() {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.canScroll(1)) {
            player.playClickSound()
            gui.scroll(1)
        }
    }
    
}