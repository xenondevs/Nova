package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.addLoreLines
import xyz.xenondevs.nova.util.localized
import xyz.xenondevs.nova.util.setLocalizedName

object UpgradesTeaserItem : BaseItem() {
    
    override fun getItemProvider(): ItemProvider {
        return NovaMaterial.UPGRADES_BUTTON.createBasicItemBuilder()
            .setLocalizedName("menu.nova.upgrades")
            .addLoreLines(localized(ChatColor.RED, "menu.nova.upgrades.coming-soon"))
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
}