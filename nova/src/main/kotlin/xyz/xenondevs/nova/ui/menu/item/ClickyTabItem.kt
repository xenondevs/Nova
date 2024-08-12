package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.TabItem
import xyz.xenondevs.nova.util.playClickSound

class ClickyTabItem(private val tab: Int, private val itemProvider: (TabGui) -> ItemProvider) : TabItem(tab) {
    
    override fun getItemProvider(gui: TabGui): ItemProvider {
        return itemProvider(gui)
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.currentTab != tab) {
            player.playClickSound()
            gui.setTab(tab)
        }
    }
    
}