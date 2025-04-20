package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

/**
 * An ui item for visualizing regions via [VisualRegion].
 *
 * @param regionUuid the [UUID] of the region to visualize
 * @param getRegion a function to receive the [Region]
 */
class VisualizeRegionItem(
    private val regionUuid: UUID,
    private val getRegion: () -> Region,
) : AbstractItem() {
    
    @Deprecated(message = "Player is redundant", replaceWith = ReplaceWith("VisualizeRegionItem(regionUUID, getRegion)"))
    constructor(player: Player, regionUUID: UUID, getRegion: () -> Region) : this(regionUUID, getRegion)
    
    override fun getItemProvider(player: Player): ItemProvider {
        val visible = VisualRegion.isVisible(player, regionUuid)
        return if (visible) DefaultGuiItems.AREA_BTN_ON.clientsideProvider
        else DefaultGuiItems.AREA_BTN_OFF.clientsideProvider
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        player.playClickSound()
        VisualRegion.toggleView(player, regionUuid, getRegion())
        notifyWindows()
    }
    
}