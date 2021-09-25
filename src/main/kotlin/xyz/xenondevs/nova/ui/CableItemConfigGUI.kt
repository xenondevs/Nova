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
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.ui.config.BUTTON_COLORS
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.notifyWindows

class CableItemConfigGUI(
    var itemHolder: ItemHolder?,
    private val holderFace: BlockFace,
    insertFilterInventory: VirtualInventory,
    extractFilterInventory: VirtualInventory
) {
    
    private val updatableItems = ArrayList<Item>()
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
        .setStructure("" +
            "1 - - - - - - - 2" +
            "| p # # c # # P |" +
            "| d # e # i # D |" +
            "| m # E # I # M |" +
            "3 - - - - - - - 4")
        .addIngredient('i', InsertItem().also(updatableItems::add))
        .addIngredient('e', ExtractItem().also(updatableItems::add))
        .addIngredient('I', VISlotElement(insertFilterInventory, 0, NovaMaterialRegistry.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
        .addIngredient('E', VISlotElement(extractFilterInventory, 0, NovaMaterialRegistry.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
        .addIngredient('p', AddNumberItem({ 0..100 }, ::getInsertPriority, ::setInsertPriority).also(updatableItems::add))
        .addIngredient('m', RemoveNumberItem({ 0..100 }, ::getInsertPriority, ::setInsertPriority).also(updatableItems::add))
        .addIngredient('d', DisplayNumberItem(::getInsertPriority, "menu.nova.cable_config.insert_priority").also(updatableItems::add))
        .addIngredient('P', AddNumberItem({ 0..100 }, ::getExtractPriority, ::setExtractPriority).also(updatableItems::add))
        .addIngredient('M', RemoveNumberItem({ 0..100 }, ::getExtractPriority, ::setExtractPriority).also(updatableItems::add))
        .addIngredient('D', DisplayNumberItem(::getExtractPriority, "menu.nova.cable_config.extract_priority").also(updatableItems::add))
        .addIngredient('c', SwitchChannelItem().also(updatableItems::add))
        .build()
    
    private fun getInsertPriority() =
        itemHolder!!.insertPriorities[holderFace]!!
    
    private fun setInsertPriority(priority: Int) =
        updateState { it.insertPriorities[holderFace] = priority }
    
    private fun getExtractPriority() =
        itemHolder!!.extractPriorities[holderFace]!!
    
    private fun setExtractPriority(priority: Int) =
        updateState { it.extractPriorities[holderFace] = priority }
    
    fun updateButtons() =
        updatableItems.notifyWindows()
    
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
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            updateState { it.insertState = !it.insertState }
            notifyWindows()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
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
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            updateState { it.extractState = !it.extractState }
            notifyWindows()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
        
    }
    
    private inner class SwitchChannelItem : BaseItem() {
        
        private var ItemHolder.channel: Int
            get() = channels[holderFace]!!
            set(value) {
                channels[holderFace] = value
            }
        
        override fun getItemProvider(): ItemProvider {
            val channel = itemHolder!!.channel
            return BUTTON_COLORS[channel].createBasicItemBuilder()
                .setDisplayName(TranslatableComponent("menu.nova.cable_config.channel", channel + 1))
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.RIGHT || clickType == ClickType.LEFT) {
                updateState {
                    var channel = it.channel
                    if (clickType == ClickType.LEFT) channel++ else channel--
                    if (channel >= ItemNetwork.CHANNEL_AMOUNT) channel = 0
                    else if (channel < 0) channel = ItemNetwork.CHANNEL_AMOUNT - 1
                    it.channel = channel
                }
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
        }
        
    }
    
}