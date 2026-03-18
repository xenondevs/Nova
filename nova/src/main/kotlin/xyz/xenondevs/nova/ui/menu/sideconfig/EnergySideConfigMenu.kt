package xyz.xenondevs.nova.ui.menu.sideconfig

import org.bukkit.block.BlockFace
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder

class EnergySideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: EnergyHolder
) : AbstractSideConfigMenu<EnergyHolder>(endPoint, DefaultNetworkTypes.ENERGY, holder) {
    
    override val gui = gui(
        ". . . . . . . .",
        ". . . . u . . .",
        ". . . l f r . .",
        ". . . . d b . ."
    ) {
        'u' by connectionConfigItem(BlockSide.TOP)
        'l' by connectionConfigItem(BlockSide.LEFT)
        'f' by connectionConfigItem(BlockSide.FRONT)
        'r' by connectionConfigItem(BlockSide.RIGHT)
        'd' by connectionConfigItem(BlockSide.BOTTOM)
        'b' by connectionConfigItem(BlockSide.BACK)
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