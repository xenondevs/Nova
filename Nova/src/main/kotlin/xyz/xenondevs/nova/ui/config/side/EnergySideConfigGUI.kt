package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.structure.Structure
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.BlockSide

internal class EnergySideConfigGUI(
    private val energyHolder: EnergyHolder
) : BaseSideConfigGUI(energyHolder) {
    
    private val structure = Structure("" +
        "# # # # u # # # #" +
        "# # # l f r # # #" +
        "# # # # d b # # #")
        .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
        .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
        .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
        .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
        .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
    
    init {
        applyStructure(structure)
    }
    
    override fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean {
        NetworkManager.execute { // TODO: runSync / runAsync ?
            it.removeEndPoint(energyHolder.endPoint, false)
            
            val allowedTypes = energyHolder.allowedConnectionType.included
            val currentType = energyHolder.connectionConfig[blockFace]!!
            var index = allowedTypes.indexOf(currentType)
            if (forward) index++ else index--
            if (index < 0) index = allowedTypes.lastIndex
            else if (index == allowedTypes.size) index = 0
            energyHolder.connectionConfig[blockFace] = allowedTypes[index]
            
            it.addEndPoint(energyHolder.endPoint, false)
                .thenRun { energyHolder.endPoint.updateNearbyBridges() }
        }
        
        return true
    }
    
    override fun getConnectionType(blockFace: BlockFace): NetworkConnectionType {
        // TODO: surround with NetworkManager lock
        return energyHolder.connectionConfig[blockFace]!!
    }
    
}