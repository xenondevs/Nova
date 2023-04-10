package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.dropItem

internal abstract class ItemStorageVanillaTileEntity internal constructor(
    blockState: VanillaTileEntityState
) : NetworkedVanillaTileEntity(blockState) {
    
    abstract val itemHolder: ItemHolder
    final override val holders: MutableMap<NetworkType, EndPointDataHolder>
        by lazy { hashMapOf(DefaultNetworkTypes.ITEMS to itemHolder) }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload) {
            val centerLocation = location.clone().add(0.5, 0.0, 0.5)
            (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
                .map(ItemFilter::createFilterItem)
                .forEach { centerLocation.dropItem(it) }
            
            itemHolder.insertFilters.clear()
            itemHolder.extractFilters.clear()
        }
    }
    
    override fun saveData() {
        super.saveData()
        itemHolder.saveData()
    }
    
}