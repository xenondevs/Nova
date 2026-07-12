package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.nova.world.*

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.itemTypeOrNull

/**
 * Simple block drop logic for non-tile-entity blocks that drops [NovaBlock.item].
 * Should not be used for tile-entity blocks.
 *
 * @see TileEntityDrops
 */
object BlockDrops : BlockBehavior {
    
    override fun getDrops(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (!ctx[BlockBreak.BLOCK_DROPS])
            return emptyList()
        return state.blockType.itemTypeOrNull?.createItemStack()?.let(::listOf) ?: emptyList()
    }
    
}
