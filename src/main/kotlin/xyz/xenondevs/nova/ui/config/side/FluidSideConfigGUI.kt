package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLoreLines

class FluidSideConfigGUI(
    val fluidHolder: FluidHolder,
    inventories: List<Pair<FluidContainer, String>>
) : InventorySideConfigGUI() {
    
    private val containers: List<FluidContainer> =
        inventories.map { it.first }
    
    private val allowedTypes: Map<FluidContainer, List<NetworkConnectionType>> =
        fluidHolder.allowedConnectionTypes.mapValues { (_, type) -> type.included }
    
    private val buttonBuilders: Map<FluidContainer, ItemBuilder> =
        inventories.withIndex().associate { (index, triple) ->
            triple.first to BUTTON_COLORS[index]
                .createBasicItemBuilder()
                .addLoreLines(TranslatableComponent(triple.second).apply { color = ChatColor.AQUA })
        }
    
    init {
        initGUI()
    }
    
    override fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean {
        NetworkManager.runNow { // TODO: runSync / runAsync ?
            it.handleEndPointRemove(fluidHolder.endPoint, true)
            
            val allowedTypes = allowedTypes[fluidHolder.containerConfig[blockFace]!!]!!
            val currentType = fluidHolder.connectionConfig[blockFace]!!
            var index = allowedTypes.indexOf(currentType)
            if (forward) index++ else index--
            if (index < 0) index = allowedTypes.lastIndex
            else if (index == allowedTypes.size) index = 0
            fluidHolder.connectionConfig[blockFace] = allowedTypes[index]
            
            it.handleEndPointAdd(fluidHolder.endPoint, false)
            fluidHolder.endPoint.updateNearbyBridges()
        }
        
        return true
    }
    
    override fun changeInventory(blockFace: BlockFace, forward: Boolean): Boolean {
        if (containers.size < 2) return false
        
        NetworkManager.runNow { // TODO: runSync / runAsync ?
            it.handleEndPointRemove(fluidHolder.endPoint, false)
            
            val currentContainer = fluidHolder.containerConfig[blockFace]!!
            var index = containers.indexOf(currentContainer)
            if (forward) index++ else index--
            if (index < 0) index = containers.lastIndex
            else if (index == containers.size) index = 0
            
            val newContainer = containers[index]
            fluidHolder.containerConfig[blockFace] = newContainer
            
            val allowedTypes = allowedTypes[newContainer]!!
            if (!allowedTypes.contains(fluidHolder.connectionConfig[blockFace]!!)) {
                fluidHolder.connectionConfig[blockFace] = allowedTypes[0]
            }
            
            it.handleEndPointAdd(fluidHolder.endPoint)
        }
        
        return true
    }
    
    override fun getBlockFace(blockSide: BlockSide): BlockFace {
        return (fluidHolder.endPoint as TileEntity).getFace(blockSide)
    }
    
    override fun getConnectionType(blockFace: BlockFace): NetworkConnectionType {
        // TODO: surround with NetworkManager lock
        return fluidHolder.connectionConfig[blockFace]!!
    }
    
    override fun getInventoryButtonBuilder(blockFace: BlockFace): ItemBuilder {
        // TODO: surround with NetworkManager lock
        return buttonBuilders[fluidHolder.containerConfig[blockFace]!!]!!
    }
    
}