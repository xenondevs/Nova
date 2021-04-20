package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.controlitem.TabItem
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage

class CableItemConfigGUI(private val itemStorage: ItemStorage, private val face: BlockFace) {
    
    private val itemsGUI: GUI = GUIBuilder(GUIType.NORMAL, 8, 3)
        .setStructure("" +
            "# # # # # # # #" +
            "# i # # # e # #" +
            "# # # # # # # #")
        .addIngredient('i', InsertItem())
        .addIngredient('e', ExtractItem())
        .build()
    
    private val mainGUI = GUIBuilder(GUIType.TAB, 9, 3)
        .setStructure("" +
            "i x x x x x x x x" +
            "# x x x x x x x x" +
            "# x x x x x x x x")
        .addGUI(itemsGUI)
        .addIngredient('i', TabItem(0) {
            if (it.currentTab == 0) NovaMaterial.ITEM_OFF_BUTTON.createBasicItemBuilder()
            else NovaMaterial.ITEM_ON_BUTTON.createBasicItemBuilder().setDisplayName("§rItems")
        })
        .build()
    
    fun openWindow(player: Player) {
        SimpleWindow(player, "Config", mainGUI).show()
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
                .createBasicItemBuilder().setDisplayName("§rInsert")
        
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
                .createBasicItemBuilder().setDisplayName("§rExtract")
        
        override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) {
            updateState { state = !state }
            notifyWindows()
        }
        
    }
    
}