package xyz.xenondevs.nova.ui.menu.sideconfig

import org.bukkit.block.BlockFace
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.BlockSide

class EnergySideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: EnergyHolder
) : AbstractSideConfigMenu<EnergyHolder>(endPoint, DefaultNetworkTypes.ENERGY, holder) {
    
    init {
        gui.applyStructure(
            Structure(
                "# # # # u # # # #",
                "# # # l f r # # #",
                "# # # # d b # # #")
                .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
                .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
                .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
                .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
                .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
                .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
        )
    }
    
    override fun getAllowedConnectionType(face: BlockFace): NetworkConnectionType {
        if (face in holder.blockedFaces)
            return NetworkConnectionType.NONE
        
        return holder.allowedConnectionType
    }
    
    override fun getConnectionType(face: BlockFace): NetworkConnectionType {
        return holder.connectionConfig[face]!!
    }
    
    override fun setConnectionType(face: BlockFace, type: NetworkConnectionType) {
        holder.connectionConfig[face] = type
    }
    
}