package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

class VisualizeRegionItem(
    private val regionUUID: UUID,
    private val region: Region
) : BaseItem() {
    
    override fun getItemProvider(): ItemProvider {
        return object : ItemProvider {
            
            override fun get(): ItemStack? {
                return null
            }
            
            override fun getFor(playerUUID: UUID): ItemStack {
                val visible = VisualRegion.isVisible(playerUUID, regionUUID)
                return (if (visible) NovaMaterial.AREA_ON_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.visual_region.hide")
                else NovaMaterial.AREA_OFF_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.visual_region.show")).getFor(playerUUID)
            }
            
        }
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        VisualRegion.toggleView(player, regionUUID, region)
        notifyWindows()
    }
    
}