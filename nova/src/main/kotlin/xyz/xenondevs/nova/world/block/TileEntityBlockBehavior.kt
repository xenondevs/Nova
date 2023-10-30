package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.util.runTask

open class TileEntityBlockBehavior protected constructor(private val interactive: Boolean) : BlockBehavior.Default<NovaTileEntityState>() {
    
    override fun handleInteract(state: NovaTileEntityState, ctx: Context<BlockInteract>): Boolean {
        if (interactive) {
            val sourcePlayer = ctx[ContextParamTypes.SOURCE_ENTITY] as? Player
            if (sourcePlayer != null)
                runTask { sourcePlayer.swingMainHand() }
            
            return state.tileEntity.handleRightClick(ctx)
        }
        
        return false
    }
    
    override fun getDrops(state: NovaTileEntityState, ctx: Context<BlockBreak>): List<ItemStack> {
        val sourceEntity: Entity? = ctx[ContextParamTypes.SOURCE_ENTITY]
        return state.tileEntity.getDrops(sourceEntity !is Player || sourceEntity.gameMode != GameMode.CREATIVE)
    }
    
    override fun getExp(state: NovaTileEntityState, ctx: Context<BlockBreak>): Int {
        return state.tileEntity.getExp()
    }
    
    companion object {
        
        val INTERACTIVE = TileEntityBlockBehavior(true)
        val NON_INTERACTIVE = TileEntityBlockBehavior(false)
        
    }
    
}