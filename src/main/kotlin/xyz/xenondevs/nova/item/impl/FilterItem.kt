package xyz.xenondevs.nova.item.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.util.SlotUtils
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.item.ItemFilter
import xyz.xenondevs.nova.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson

private val ITEM_FILTER_KEY = NamespacedKey(NOVA, "itemFilter")

fun ItemStack.getFilterConfig(): ItemFilter? =
    GSON.fromJson(itemMeta!!.persistentDataContainer.get(ITEM_FILTER_KEY, JsonElementDataType))

fun ItemStack.saveFilterConfig(itemFilter: ItemFilter) {
    val itemMeta = itemMeta!!
    itemMeta.persistentDataContainer.set(ITEM_FILTER_KEY, JsonElementDataType, GSON.toJsonTree(itemFilter))
    setItemMeta(itemMeta)
}

object FilterItem : NovaItem() {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_AIR) {
            event.isCancelled = true
            ItemFilterGUI(player, itemStack).openWindow()
        }
    }
}

private class ItemFilterGUI(private val player: Player, private val itemStack: ItemStack) {
    
    private val itemFilter = itemStack.getFilterConfig() ?: ItemFilter(true, arrayOfNulls(7))
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 4)
        .setStructure("" +
            "1 - - - - - - - 2" +
            "| # # # m # # # |" +
            "| . . . . . . . |" +
            "3 - - - - - - - 4")
        .addIngredient('m', SwitchModeItem())
        .build()
        .also { gui ->
            SlotUtils.getSlotsRect(1, 2, 7, 1, 9)
                .withIndex()
                .forEach { (configIndex, slot) -> gui.setItem(slot, FilteredItem(configIndex)) }
        }
    
    fun openWindow() {
        SimpleWindow(player, "Item Filter", gui).show()
    }
    
    private fun saveFilterConfig() = itemStack.saveFilterConfig(itemFilter)
    
    private inner class FilteredItem(private val configIndex: Int) : BaseItem() {
        
        private var itemStack: ItemStack? = itemFilter.items[configIndex]
        
        override fun getItemBuilder(): ItemBuilder {
            return object : ItemBuilder(Material.AIR) {
                override fun build() = itemStack
            }
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val cursorItem = event.cursor
            
            if (cursorItem != null)
                itemStack = cursorItem.clone().apply { amount = 1 }
            else itemStack = null
            
            itemFilter.items[configIndex] = itemStack.takeUnless { it?.type == Material.AIR }
            
            saveFilterConfig()
            notifyWindows()
        }
        
    }
    
    private inner class SwitchModeItem : BaseItem() {
        
        override fun getItemBuilder(): ItemBuilder =
            if (itemFilter.whitelist) NovaMaterial.WHITELIST_BUTTON.createBasicItemBuilder().setDisplayName("§rCurrently: Whitelist")
            else NovaMaterial.BLACKLIST_BUTTON.createBasicItemBuilder().setDisplayName("§rCurrently: Blacklist")
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            itemFilter.whitelist = !itemFilter.whitelist
            saveFilterConfig()
            notifyWindows()
        }
        
    }
    
}