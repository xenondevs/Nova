package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial

object UpgradesTeaserItem : BaseItem() {
    override fun getItemBuilder(): ItemBuilder {
        return NovaMaterial.UPGRADES_BUTTON.createBasicItemBuilder()
            .setLocalizedName("menu.nova.upgrades")
            .setLore(listOf("§cComing soon!"))
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
}