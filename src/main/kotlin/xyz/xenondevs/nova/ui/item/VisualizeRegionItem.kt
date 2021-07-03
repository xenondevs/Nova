package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.region.VisualRegion
import java.util.*

class VisualizeRegionItem(
    private val regionUUID: UUID,
    private val pos1: Location,
    private val pos2: Location
) : BaseItem() {
    
    override fun getItemBuilder(): ItemBuilder {
        return object : ItemBuilder(Material.AIR) {
            
            override fun buildFor(playerUUID: UUID): ItemStack {
                val visible = VisualRegion.isVisible(playerUUID, regionUUID)
                return (if (visible) NovaMaterial.AREA_ON_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.visual_region.hide")
                else NovaMaterial.AREA_OFF_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.visual_region.show")).buildFor(playerUUID)
            }
            
        }
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        VisualRegion.toggleView(player, regionUUID, pos1, pos2)
        notifyWindows()
    }
    
}