package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.item.NovaItemBuilder
import xyz.xenondevs.nova.tileentity.impl.StorageUnit.StorageUnitInventory

class StorageUnitDisplay(private val unitInventory: StorageUnitInventory) : BaseItem() {
    
    override fun getItemBuilder(): ItemBuilder {
        val type = unitInventory.type ?: return ItemBuilder(Material.BARRIER).setDisplayName("§r")
        val amount = unitInventory.amount
        val component = TranslatableComponent(
            "menu.nova.storage_unit.item_display_" + if (amount > 1) "plural" else "singular",
            TextComponent(amount.toString()).apply { color = ChatColor.GREEN }
        )
        return NovaItemBuilder(type).setLocalizedName(component).setAmount(1)
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}