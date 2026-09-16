package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.BlockEntity
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder

internal abstract class ItemStorageVanillaTileEntity(
    blockEntity: BlockEntity
) : NetworkedVanillaTileEntity(blockEntity) {
    
    abstract val itemHolder: ItemHolder
    
    final override val holders: Set<EndPointDataHolder> by lazy { setOf(itemHolder) }
    
    internal fun handleBreak() {
        val centerLocation = block.location.add(0.5, 0.0, 0.5)
        (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
            .map(ItemFilter<*>::toItemStack)
            .forEach(centerLocation::dropItem)
        
        itemHolder.insertFilters = CubeFaceMap.NULL
        itemHolder.extractFilters = CubeFaceMap.NULL
    }
    
}
