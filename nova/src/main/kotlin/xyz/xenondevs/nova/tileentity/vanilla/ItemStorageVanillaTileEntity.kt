package xyz.xenondevs.nova.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.world.BlockPos

internal abstract class ItemStorageVanillaTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(pos, data) {
    
    abstract val itemHolder: ItemHolder
    final override val holders: Set<EndPointDataHolder> by lazy { setOf(itemHolder) }
    
    override fun handleBreak() {
        super.handleBreak()
        val centerLocation = pos.location.add(0.5, 0.0, 0.5)
        (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
            .map(ItemFilter::createFilterItem)
            .forEach { centerLocation.dropItem(it) }
        
        itemHolder.insertFilters.clear()
        itemHolder.extractFilters.clear()
    }
    
}