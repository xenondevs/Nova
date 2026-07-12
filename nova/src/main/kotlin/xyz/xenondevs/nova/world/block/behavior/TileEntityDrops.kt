package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.nova.world.*

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

/**
 * Delegates drops and experience to [TileEntity.getDrops] and [TileEntity.getExp].
 * Should only be used for tile-entity blocks.
 *
 * @see BlockDrops
 */
object TileEntityDrops : BlockBehavior {
    
    override fun getDrops(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (!ctx[BlockBreak.BLOCK_DROPS] && !ctx[BlockBreak.BLOCK_STORAGE_DROPS])
            return emptyList()
        
        return block.novaTileEntity
            ?.getDrops(ctx[BlockBreak.BLOCK_DROPS])
            ?: emptyList()
    }
    
    override fun getExp(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>): Int {
        return block.novaTileEntity?.getExp() ?: 0
    }
    
}
