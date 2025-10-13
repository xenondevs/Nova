package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Delegates drops and experience to [TileEntity.getDrops] and [TileEntity.getExp].
 * Should only be used for tile-entity blocks.
 *
 * @see BlockDrops
 */
object TileEntityDrops : BlockBehavior {
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (!ctx[BlockBreak.BLOCK_DROPS] && !ctx[BlockBreak.BLOCK_STORAGE_DROPS])
            return emptyList()
        
        return WorldDataManager.getTileEntity(pos)
            ?.getDrops(ctx[BlockBreak.BLOCK_DROPS])
            ?: emptyList()
    }
    
    override fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int {
        return WorldDataManager.getTileEntity(pos)?.getExp() ?: 0
    }
    
}