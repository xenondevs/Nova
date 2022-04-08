package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.enumMapOf

abstract class ItemStorageVanillaTileEntity internal constructor(
    blockState: VanillaTileEntityState
) : NetworkedVanillaTileEntity(blockState) {
    
    abstract val itemHolder: ItemHolder
    final override val holders: MutableMap<NetworkType, EndPointDataHolder>
        by lazy { enumMapOf(NetworkType.ITEMS to itemHolder) }
    
    override fun saveData() {
        super.saveData()
        itemHolder.saveData()
    }
    
}