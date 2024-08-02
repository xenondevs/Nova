package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
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
        if (ctx[DefaultContextParamTypes.SOURCE_PLAYER]?.gameMode == GameMode.CREATIVE)
            return emptyList()
        
        return state.block.item
            ?.let { listOf(it.createItemStack()) }
            ?: return emptyList()
    }
    
}