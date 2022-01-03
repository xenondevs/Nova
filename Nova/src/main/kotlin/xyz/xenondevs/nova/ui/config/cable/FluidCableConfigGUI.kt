package xyz.xenondevs.nova.ui.config.cable

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.fluid.FluidNetwork
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem

class FluidCableConfigGUI(
    val fluidHolder: FluidHolder,
    private val face: BlockFace
) : BaseCableConfigGUI(FluidNetwork.CHANNEL_AMOUNT) {
    
    val gui: GUI
    
    init {
        updateValues(false)
        
        gui = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                "# p # # # # # P #" +
                "# d # e c i # D #" +
                "# m # # # # # M #")
            .addIngredient('i', InsertItem().also(updatableItems::add))
            .addIngredient('e', ExtractItem().also(updatableItems::add))
            .addIngredient('P', AddNumberItem({ 0..100 }, { insertPriority }, { insertPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('M', RemoveNumberItem({ 0..100 }, { insertPriority }, { insertPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('D', DisplayNumberItem({ insertPriority }, "menu.nova.cable_config.insert_priority").also(updatableItems::add))
            .addIngredient('p', AddNumberItem({ 0..100 }, { extractPriority }, { extractPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('m', RemoveNumberItem({ 0..100 }, { extractPriority }, { extractPriority = it; updateButtons() }).also(updatableItems::add))
            .addIngredient('d', DisplayNumberItem({ extractPriority }, "menu.nova.cable_config.extract_priority").also(updatableItems::add))
            .addIngredient('c', SwitchChannelItem().also(updatableItems::add))
            .build()
    }
    
    override fun updateValues(updateButtons: Boolean) {
        NetworkManager.runNow { // TODO: runSync / runAsync ?
            val allowedConnections = fluidHolder.allowedConnectionTypes[fluidHolder.containerConfig[face]]!!
            allowsExtract = allowedConnections.extract
            allowsInsert = allowedConnections.insert
            
            insertPriority = fluidHolder.insertPriorities[face]!!
            extractPriority = fluidHolder.extractPriorities[face]!!
            insertState = fluidHolder.connectionConfig[face]!!.insert
            extractState = fluidHolder.connectionConfig[face]!!.extract
            channel = fluidHolder.channels[face]!!
        }
        
        if (updateButtons) updateButtons()
    }
    
    override fun writeChanges() {
        NetworkManager.runAsync {
            if (fluidHolder.endPoint.networks.isNotEmpty()) {
                it.handleEndPointRemove(fluidHolder.endPoint, true)
                
                fluidHolder.insertPriorities[face] = insertPriority
                fluidHolder.extractPriorities[face] = extractPriority
                fluidHolder.channels[face] = channel
                fluidHolder.connectionConfig[face] = NetworkConnectionType.of(insertState, extractState)
                
                it.handleEndPointAdd(fluidHolder.endPoint, false)
                fluidHolder.endPoint.updateNearbyBridges()
            }
        }
    }
    
}