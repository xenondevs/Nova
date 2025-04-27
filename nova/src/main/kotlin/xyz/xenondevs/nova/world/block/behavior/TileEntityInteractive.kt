package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.entity.Player
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.player.swingMainHandEventless

/**
 * Delegates interactions to [TileEntity.handleRightClick].
 * Should only be used for tile-entity blocks.
 */
object TileEntityInteractive : BlockBehavior {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val sourcePlayer = ctx[DefaultContextParamTypes.SOURCE_ENTITY] as? Player
        if (sourcePlayer != null)
            runTask { sourcePlayer.swingMainHandEventless() } // TODO: runTask required?
        
        return WorldDataManager.getTileEntity(pos)?.handleRightClick(ctx) ?: false
    }
    
}