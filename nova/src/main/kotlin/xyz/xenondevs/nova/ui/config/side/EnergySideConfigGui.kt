package xyz.xenondevs.nova.ui.config.side

import org.bukkit.block.BlockFace
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.BlockSide

internal class EnergySideConfigGui(
    holder: EnergyHolder
) : AbstractSideConfigGui<EnergyHolder>(holder) {
    
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
    
    override fun getAllowedConnectionTypes(blockFace: BlockFace): List<NetworkConnectionType> {
        return holder.allowedConnectionType.included
    }
    
}