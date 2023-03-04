package xyz.xenondevs.nova.ui.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

class VisualizeRegionItem(
    private val regionUUID: UUID,
    private val getRegion: () -> Region,
) : AbstractItem() {
    
    // FIXME
    override fun getItemProvider() = CoreGuiMaterial.AREA_BTN_OFF.clientsideProvider
//        object : ItemProvider {
//            override fun get() = null
//            override fun getFor(playerUUID: UUID): ItemStack {
//                val visible = VisualRegion.isVisible(playerUUID, regionUUID)
//                return (if (visible) CoreGuiMaterial.AREA_BTN_ON.clientsideProvider
//                else CoreGuiMaterial.AREA_BTN_OFF.clientsideProvider).get()
//            }
//        }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        VisualRegion.toggleView(player, regionUUID, getRegion())
        notifyWindows()
    }
    
}