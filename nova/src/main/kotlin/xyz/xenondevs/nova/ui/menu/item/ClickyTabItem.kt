package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.AbstractTabGuiBoundItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound

class ClickyTabItem(private val tab: Int, private val itemProvider: (TabGui) -> ItemProvider) : AbstractTabGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider {
        return itemProvider(gui)
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.tab != tab) {
            player.playClickSound()
            gui.tab = tab
        }
    }
    
}