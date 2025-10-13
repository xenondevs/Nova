package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState

/**
 * Simple block drop logic for non-tile-entity blocks.
 * Should not be used for tile-entity blocks.
 *
 * @see TileEntityDrops
 */
object BlockDrops : BlockBehavior {
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (!ctx[BlockBreak.BLOCK_DROPS])
            return emptyList()
        
        return state.block.item
            ?.let { listOf(it.createItemStack()) }
            ?: return emptyList()
    }
    
}