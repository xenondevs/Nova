package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
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
import xyz.xenondevs.nova.item.impl.getOrCreateFilterConfig
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.ui.config.BUTTON_COLORS
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.lockAndRun
import xyz.xenondevs.nova.util.notifyWindows
import xyz.xenondevs.nova.util.putOrRemove
import xyz.xenondevs.nova.util.runAsyncTask

class CableConfigGUI(
    val itemHolder: ItemHolder,
    private val face: BlockFace
) {
    
    private val gui: GUI
    private val updatableItems = ArrayList<Item>()
    private val insertFilterInventory = VirtualInventory(null, 1, arrayOfNulls(1), intArrayOf(1))
    private val extractFilterInventory = VirtualInventory(null, 1, arrayOfNulls(1), intArrayOf(1))
    
    private var insertPriority = -1
    private var extractPriority = -1
    private var insertState = false
    private var extractState = false
    private var channel = -1
    
    init {
        updateValues(false)
        
        gui = GUIBuilder(GUIType.NORMAL, 9, 5)
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
            .addIngredient('P', AddNumberItem({ 0..100 }, { insertPriority }, { insertPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('M', RemoveNumberItem({ 0..100 }, { insertPriority }, { insertPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('D', DisplayNumberItem({ insertPriority }, "menu.nova.cable_config.insert_priority").also(updatableItems::add))
            .addIngredient('p', AddNumberItem({ 0..100 }, { extractPriority }, { extractPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('m', RemoveNumberItem({ 0..100 }, { extractPriority }, { extractPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('d', DisplayNumberItem({ extractPriority }, "menu.nova.cable_config.extract_priority").also(updatableItems::add))
            .addIngredient('c', SwitchChannelItem().also(updatableItems::add))
            .build()
    }
    
    fun updateValues(updateButtons: Boolean = true) {
        NetworkManager.LOCK.lockAndRun {
            insertPriority = itemHolder.insertPriorities[face]!!
            extractPriority = itemHolder.extractPriorities[face]!!
            insertState = itemHolder.itemConfig[face]!!.insert
            extractState = itemHolder.itemConfig[face]!!.extract
            channel = itemHolder.channels[face]!!
            
            insertFilterInventory.setItemStackSilently(0, itemHolder.insertFilters[face]?.createFilterItem())
            extractFilterInventory.setItemStackSilently(0, itemHolder.extractFilters[face]?.createFilterItem())
        }
        
        if (updateButtons) updateButtons()
    }
    
    private fun updateButtons() = updatableItems.notifyWindows()
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.cable_config")), gui)
            .also { it.addCloseHandler(::writeChanges) }
            .show()
    }
    
    fun closeForAllViewers() {
        gui.closeForAllViewers()
    }
    
    private fun writeChanges() {
        runAsyncTask {
            NetworkManager.LOCK.lockAndRun {
                if (itemHolder.endPoint.networks.isNotEmpty()) {
                    NetworkManager.handleEndPointRemove(itemHolder.endPoint, true)
                    
                    itemHolder.insertPriorities[face] = insertPriority
                    itemHolder.extractPriorities[face] = extractPriority
                    itemHolder.channels[face] = channel
                    itemHolder.setInsert(face, insertState)
                    itemHolder.setExtract(face, extractState)
                    itemHolder.insertFilters.putOrRemove(face, insertFilterInventory.getUnsafeItemStack(0)?.getOrCreateFilterConfig())
                    itemHolder.extractFilters.putOrRemove(face, extractFilterInventory.getUnsafeItemStack(0)?.getOrCreateFilterConfig())
                    
                    NetworkManager.handleEndPointAdd(itemHolder.endPoint)
                    itemHolder.endPoint.updateNearbyBridges() // required as bridges are not updated during handleEndPointRemove
                }
            }
        }
    }
    
    private inner class InsertItem : BaseItem() {
        
        override fun getItemProvider(): ItemProvider =
            (if (insertState) NovaMaterialRegistry.GREEN_BUTTON else NovaMaterialRegistry.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.insert")
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            insertState = !insertState
            notifyWindows()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
        
    }
    
    private inner class ExtractItem : BaseItem() {
        
        override fun getItemProvider(): ItemProvider =
            (if (extractState) NovaMaterialRegistry.GREEN_BUTTON else NovaMaterialRegistry.GRAY_BUTTON)
                .createBasicItemBuilder().setLocalizedName("menu.nova.cable_config.extract")
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            extractState = !extractState
            notifyWindows()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
        
    }
    
    private inner class SwitchChannelItem : BaseItem() {
        
        override fun getItemProvider(): ItemProvider {
            return BUTTON_COLORS[channel].createBasicItemBuilder()
                .setDisplayName(TranslatableComponent("menu.nova.cable_config.channel", channel + 1))
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.RIGHT || clickType == ClickType.LEFT) {
                if (clickType == ClickType.LEFT) channel++ else channel--
                if (channel >= ItemNetwork.CHANNEL_AMOUNT) channel = 0
                else if (channel < 0) channel = ItemNetwork.CHANNEL_AMOUNT - 1
                
                notifyWindows()
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
        }
        
    }
    
}