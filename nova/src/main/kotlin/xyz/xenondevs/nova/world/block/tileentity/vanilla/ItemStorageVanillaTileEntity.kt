package xyz.xenondevs.nova.world.block.tileentity.vanilla

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder

internal abstract class ItemStorageVanillaTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(type, pos, data) {
    
    abstract val itemHolder: ItemHolder
    final override val holders: Set<EndPointDataHolder> by lazy { setOf(itemHolder) }
    
    override fun handleBreak() {
        super.handleBreak()
        val centerLocation = pos.location.add(0.5, 0.0, 0.5)
        (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
            .map(ItemFilter<*>::toItemStack)
            .forEach { centerLocation.dropItem(it) }
        
        itemHolder.insertFilters.clear()
        itemHolder.extractFilters.clear()
    }
    
}