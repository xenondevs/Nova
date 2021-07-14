package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage

class CableItemConfigGUI(
    private val itemStorage: ItemStorage,
    private val face: BlockFace,
    insertFilterInventory: VirtualInventory,
    extractFilterInventory: VirtualInventory
) {
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 4)
        .setStructure("" +
            "# # # # # # # # #" +
            "# # i # # # e # #" +
            "# # 1 # # # 2 # #" +
            "# # # # # # # # #")
        .addIngredient('i', InsertItem())
        .addIngredient('e', ExtractItem())
        .addIngredient('1', VISlotElement(insertFilterInventory, 0))
        .addIngredient('2', VISlotElement(extractFilterInventory, 0))
        .build()
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.cable_config")), gui).show()
    }
    
    private fun updateState(run: () -> Unit) {
        NetworkManager.handleEndPointRemove(itemStorage, false)
        run()
        if (itemStorage.itemConfig[face]!! != ItemConnectionType.NONE) {
            NetworkManager.handleEndPointAdd(itemStorage)
        }
    }
    
    private inner class InsertItem : BaseItem() {
        
        private var state: Boolean
            get() = itemStorage.itemConfig[face]!!.insert
            set(value) = itemStorage.setInsert(face, value)
        
        override fun getItemBuilder(): ItemBuilder =
            (if (state) NovaMaterial.GREEN_BUTTON else NovaMaterial.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.insert")
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) {
            updateState { state = !state }
            notifyWindows()
        }
        
    }
    
    private inner class ExtractItem : BaseItem() {
        
        private var state: Boolean
            get() = itemStorage.itemConfig[face]!!.extract
            set(value) = itemStorage.setExtract(face, value)
        
        override fun getItemBuilder(): ItemBuilder =
            (if (state) NovaMaterial.GREEN_BUTTON else NovaMaterial.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.extract")
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) {
            updateState { state = !state }
            notifyWindows()
        }
        
    }
    
}