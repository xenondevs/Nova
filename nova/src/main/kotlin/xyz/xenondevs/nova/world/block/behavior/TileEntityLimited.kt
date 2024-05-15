package xyz.xenondevs.nova.world.block.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * Tracks tile-entity placement and removal and enforces tile-entity limits.
 * Should only be applied to tile-entity blocks.
 */
object TileEntityLimited : BlockBehavior {
    
    override suspend fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>): Boolean {
        if (ctx[DefaultContextParamTypes.BYPASS_TILE_ENTITY_LIMITS])
            return true
        
        val result = TileEntityLimits.canPlace(ctx)
        if (!result.allowed) {
            ctx[DefaultContextParamTypes.SOURCE_PLAYER]?.sendMessage(Component.text(result.message, NamedTextColor.RED))
            return false
        }
        
        return true
    }
    
    override fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        val tileEntityBlock = state.block as NovaTileEntityBlock
        TileEntityTracker.handlePlace(tileEntityBlock, ctx)
    }
    
    override fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        val tileEntity = WorldDataManager.getTileEntity(pos) ?: return
        TileEntityTracker.handleBreak(tileEntity, ctx)
    }
    
}