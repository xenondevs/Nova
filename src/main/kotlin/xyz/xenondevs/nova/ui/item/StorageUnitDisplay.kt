package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.tileentity.impl.StorageUnit.StorageUnitInventory

class StorageUnitDisplay(val unitInventory: StorageUnitInventory) : BaseItem() {
    
    override fun getItemBuilder(): ItemBuilder {
        val type = unitInventory.type ?: return ItemBuilder(Material.BARRIER).setDisplayName("§r")
        return ItemBuilder(type).setDisplayName("§a${unitInventory.amount} §7items")
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
}