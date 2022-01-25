package xyz.xenondevs.nova.item.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.virtualinventory.event.UpdateReason
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BLACKLIST_BUTTON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.WHITELIST_BUTTON
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.util.data.setLocalizedName

private val ITEM_FILTER_KEY = NamespacedKey(NOVA, "itemFilterCBF")

fun ItemStack.getFilterConfigOrNull(): ItemFilter? {
    val container = itemMeta!!.persistentDataContainer
    return container.get(ITEM_FILTER_KEY, CompoundElementDataType)?.let(::ItemFilter)
}

fun ItemStack.getOrCreateFilterConfig(): ItemFilter = getFilterConfigOrNull() ?: ItemFilter()

fun ItemStack.saveFilterConfig(itemFilter: ItemFilter) {
    val itemMeta = itemMeta!!
    itemMeta.persistentDataContainer.set(ITEM_FILTER_KEY, CompoundElementDataType, itemFilter.compound)
    setItemMeta(itemMeta)
}

object FilterItem : NovaItem() {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_AIR) {
            event.isCancelled = true
            ItemFilterWindow(player, itemStack)
        }
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder =
        itemBuilder.addModifier {
            it.saveFilterConfig(ItemFilter())
            return@addModifier it
        }
    
}

private class ItemFilterWindow(player: Player, private val itemStack: ItemStack) {
    
    private val itemFilter = itemStack.getOrCreateFilterConfig()
    private val filterInventory = object : VirtualInventory(null, 7, itemFilter.items, IntArray(7) { 1 }) {
        
        override fun addItem(updateReason: UpdateReason?, itemStack: ItemStack): Int {
            items.withIndex()
                .firstOrNull { it.value == null }
                ?.index
                ?.also { putItemStack(updateReason, it, itemStack) }
            
            return itemStack.amount
        }
        
        override fun setItemStack(updateReason: UpdateReason?, slot: Int, itemStack: ItemStack?): Boolean {
            return super.forceSetItemStack(updateReason, slot, itemStack)
        }
        
    }
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 4)
        .setStructure("" +
            "1 - - - - - - - 2" +
            "| # # m # n # # |" +
            "| i i i i i i i |" +
            "3 - - - - - - - 4")
        .addIngredient('m', SwitchModeItem())
        .addIngredient('n', SwitchNBTItem())
        .addIngredient('i', filterInventory)
        .build()
    
    private val window = SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.item_filter")), gui)
    
    init {
        filterInventory.setItemUpdateHandler(::handleInventoryUpdate)
        window.addCloseHandler(::saveFilterConfig)
        window.show()
    }
    
    private fun saveFilterConfig() {
        itemFilter.items = filterInventory.items
        itemStack.saveFilterConfig(itemFilter)
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.updateReason == null) return
        
        event.isCancelled = true
        filterInventory.setItemStack(null, event.slot, event.newItemStack?.clone()?.apply { amount = 1 })
    }
    
    private inner class SwitchModeItem : BaseItem() {
        
        override fun getItemProvider(): ItemProvider =
            if (itemFilter.whitelist) WHITELIST_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.item_filter.whitelist")
            else BLACKLIST_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.item_filter.blacklist")
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            itemFilter.whitelist = !itemFilter.whitelist
            notifyWindows()
        }
        
    }
    
    private inner class SwitchNBTItem : BaseItem() {
        
        override fun getItemProvider(): ItemProvider {
            return (if (itemFilter.nbt) NovaMaterialRegistry.NBT_ON_BUTTON else NovaMaterialRegistry.NBT_OFF_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.item_filter.nbt." + if (itemFilter.nbt) "on" else "off")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            itemFilter.nbt = !itemFilter.nbt
            notifyWindows()
        }
        
    }
    
}