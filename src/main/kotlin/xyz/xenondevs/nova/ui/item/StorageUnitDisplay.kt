package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.tileentity.impl.StorageUnit.StorageUnitInventory

class StorageUnitDisplay(private val unitInventory: StorageUnitInventory) : BaseItem() {
    
    override fun getItemBuilder(): ItemBuilder {
        val type = unitInventory.type ?: return ItemBuilder(Material.BARRIER).setDisplayName("§r")
        val amount = unitInventory.amount
        return ItemBuilder(type).setAmount(1).setDisplayName("§a${amount} " + if (amount > 1) "§7Items" else "§7Item")
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
    
}