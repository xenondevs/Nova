package xyz.xenondevs.nova.world.block.tileentity.vanilla

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder

internal abstract class ItemStorageVanillaTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(type, pos, data) {
    
    abstract val itemHolder: ItemHolder
    final override val holders: List<EndPointDataHolder> by lazy { listOf(itemHolder) }
    
    override fun requestsLocalNetwork(face: BlockFace): Boolean =
        itemHolder.connectionConfig[face] != NetworkConnectionType.BUFFER
    
    override fun handleBreak() {
        super.handleBreak()
        val centerLocation = pos.location.add(0.5, 0.0, 0.5)
        itemHolder.insertFilters.forEach { if (it != null) centerLocation.dropItem(it.toItemStack()) }
        itemHolder.extractFilters.forEach { if (it != null) centerLocation.dropItem(it.toItemStack()) }
        
        itemHolder.insertFilters = CubeFaceMap.NULL
        itemHolder.extractFilters = CubeFaceMap.NULL
    }
    
}