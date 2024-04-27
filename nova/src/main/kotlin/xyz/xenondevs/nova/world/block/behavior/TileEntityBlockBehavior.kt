package xyz.xenondevs.nova.world.block.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

open class TileEntityBlockBehavior protected constructor() : BlockBehavior.Default() {
    
    override fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockPlace>): Boolean {
        if (ctx[ContextParamTypes.BYPASS_TILE_ENTITY_LIMITS])
            return true
        
        val result = TileEntityLimits.canPlace(ctx)
        if (!result.allowed) {
            ctx[ContextParamTypes.SOURCE_PLAYER]?.sendMessage(Component.text(result.message, NamedTextColor.RED))
            return false
        }
        
        return true
    }
    
    override fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockPlace>) {
        super.handlePlace(pos, state, ctx)
        val tileEntityBlock = state.block as NovaTileEntityBlock
        TileEntityTracker.handlePlace(tileEntityBlock, ctx)
    }
    
    override fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        val tileEntity = WorldDataManager.getTileEntity(pos) ?: return
        TileEntityTracker.handleBreak(tileEntity, ctx)
    }
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        val sourceEntity: Entity? = ctx[ContextParamTypes.SOURCE_ENTITY]
        return WorldDataManager.getTileEntity(pos)
            ?.getDrops(sourceEntity !is Player || sourceEntity.gameMode != GameMode.CREATIVE)
            ?: emptyList()
    }
    
    override fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int {
        return WorldDataManager.getTileEntity(pos)?.getExp() ?: 0
    }
    
    companion object : TileEntityBlockBehavior()
    
}

open class InteractiveTileEntityBlockBehavior protected constructor() : TileEntityBlockBehavior() {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val sourcePlayer = ctx[ContextParamTypes.SOURCE_ENTITY] as? Player
        if (sourcePlayer != null)
            runTask { sourcePlayer.swingMainHand() }
        
        return WorldDataManager.getTileEntity(pos)?.handleRightClick(ctx) ?: false
    }
    
    companion object : InteractiveTileEntityBlockBehavior()
    
}