package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Delegates [BlockBehavior.useItemOn] to [TileEntity.useItemOn] and
 * [BlockBehavior.use] to [TileEntity.use]
 */
object TileEntityInteractive : BlockBehavior {
    
    override fun useItemOn(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        return WorldDataManager.getTileEntity(pos)?.useItemOn(ctx) ?: InteractionResult.Pass
    }
    
    override fun use(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        return WorldDataManager.getTileEntity(pos)?.use(ctx) ?: InteractionResult.Pass
    }
    
}