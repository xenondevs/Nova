package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.data.setLocalizedName

class CableItemConfigGUI(
    var itemHolder: ItemHolder?,
    private val holderFace: BlockFace,
    insertFilterInventory: VirtualInventory,
    extractFilterInventory: VirtualInventory
) {
    private val buttons = ArrayList<Item>()
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 4)
        .setStructure("" +
            "# # # # # # # # #" +
            "# # i # # # e # #" +
            "# # 1 # # # 2 # #" +
            "# # # # # # # # #")
        .addIngredient('i', InsertItem().also(buttons::add))
        .addIngredient('e', ExtractItem().also(buttons::add))
        .addIngredient('1', VISlotElement(insertFilterInventory, 0, NovaMaterialRegistry.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
        .addIngredient('2', VISlotElement(extractFilterInventory, 0, NovaMaterialRegistry.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
        .build()
    
    fun updateButtons() {
        buttons.forEach(Item::notifyWindows)
    }
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.cable_config")), gui).show()
    }
    
    fun closeForAllViewers() {
        gui.closeForAllViewers()
    }
    
    private fun updateState(run: (ItemHolder) -> Unit) {
        val itemHolder = itemHolder
        if (itemHolder == null) {
            closeForAllViewers()
            return
        }
        
        NetworkManager.handleEndPointRemove(itemHolder.endPoint, false)
        run(itemHolder)
        NetworkManager.handleEndPointAdd(itemHolder.endPoint)
    }
    
    private inner class InsertItem : BaseItem() {
        
        private var ItemHolder?.insertState: Boolean
            get() = if (this != null) itemConfig[holderFace]?.insert ?: false else false
            set(value) {
                this?.setInsert(holderFace, value)
            }
        
        override fun getItemProvider(): ItemProvider =
            (if (itemHolder.insertState) NovaMaterialRegistry.GREEN_BUTTON else NovaMaterialRegistry.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.insert")
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) {
            updateState { it.insertState = !it.insertState }
            notifyWindows()
        }
        
    }
    
    private inner class ExtractItem : BaseItem() {
        
        private var ItemHolder?.extractState: Boolean
            get() = if (this != null) itemConfig[holderFace]?.extract ?: false else false
            set(value) {
                this?.setExtract(holderFace, value)
            }
        
        
        override fun getItemProvider(): ItemProvider =
            (if (itemHolder.extractState) NovaMaterialRegistry.GREEN_BUTTON else NovaMaterialRegistry.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.extract")
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) {
            updateState { it.extractState = !it.extractState }
            notifyWindows()
        }
        
    }
    
}