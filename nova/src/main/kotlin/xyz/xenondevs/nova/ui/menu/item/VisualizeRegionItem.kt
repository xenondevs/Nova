package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

/**
 * An ui item for visualizing regions via [VisualRegion].
 *
 * @param player the player to visualize the region for
 * @param regionUUID the [UUID] of the region to visualize
 * @param getRegion a function to receive the [Region]
 */
class VisualizeRegionItem(
    private val player: Player,
    private val regionUUID: UUID,
    private val getRegion: () -> Region,
) : AbstractItem() {
    
    override fun getItemProvider(player: Player): ItemProvider {
        val visible = VisualRegion.isVisible(player, regionUUID)
        return if (visible) DefaultGuiItems.AREA_BTN_ON.clientsideProvider
        else DefaultGuiItems.AREA_BTN_OFF.clientsideProvider
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        player.playClickSound()
        VisualRegion.toggleView(player, regionUUID, getRegion())
        notifyWindows()
    }
    
}