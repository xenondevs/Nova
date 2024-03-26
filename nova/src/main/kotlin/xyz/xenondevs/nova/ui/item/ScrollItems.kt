package xyz.xenondevs.nova.ui.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.util.playClickSound

class ScrollUpItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_UP_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_UP_OFF.model.clientsideProvider
) : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        super.handleClick(clickType, player, event)
    }
    
}

class ScrollDownItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_DOWN_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_DOWN_OFF.model.clientsideProvider
) : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        super.handleClick(clickType, player, event)
    }
    
}

class ScrollLeftItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_LEFT_OFF.model.clientsideProvider
) : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        super.handleClick(clickType, player, event)
    }
    
}

class ScrollRightItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_RIGHT_ON.model.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_RIGHT_OFF.model.clientsideProvider
) : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) on else off
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        super.handleClick(clickType, player, event)
    }
    
}