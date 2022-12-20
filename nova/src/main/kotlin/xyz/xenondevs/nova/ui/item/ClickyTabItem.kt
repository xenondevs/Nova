package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.controlitem.TabItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.util.playClickSound

class ClickyTabItem(private val tab: Int, private val itemProvider: (TabGUI) -> ItemProvider) : TabItem(tab) {
    
    override fun getItemProvider(gui: TabGUI): ItemProvider {
        return itemProvider(gui)
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.currentTab != tab) {
            player.playClickSound()
            gui.showTab(tab)
        }
    }
    
}