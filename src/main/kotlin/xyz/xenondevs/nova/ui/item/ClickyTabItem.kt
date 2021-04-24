package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.controlitem.TabItem
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.function.Function

class ClickyTabItem(private val tab: Int, builderFunction: Function<TabGUI, ItemBuilder>?) : TabItem(tab, builderFunction) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT) {
            if (gui.currentTab != tab) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                gui.showTab(tab)
            }
        }
    }
    
}