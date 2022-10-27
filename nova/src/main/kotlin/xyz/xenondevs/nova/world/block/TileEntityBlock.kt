package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext

open class TileEntityBlock protected constructor(private val interactive: Boolean) : NovaBlock.Default<NovaTileEntityState>() {
    
    override fun handleInteract(state: NovaTileEntityState, ctx: BlockInteractContext): Boolean {
        if (interactive) {
            if (ctx.source is Player)
                runTask { ctx.source.swingMainHand() }
            
            return state.tileEntity.handleRightClick(ctx)
        }
        
        return false
    }
    
    override fun getDrops(state: NovaTileEntityState, ctx: BlockBreakContext): List<ItemStack> {
        return state.tileEntity.getDrops(ctx.source !is Player || ctx.source.gameMode != GameMode.CREATIVE)
    }
    
    override fun getExp(state: NovaTileEntityState, ctx: BlockBreakContext): Int {
        return state.tileEntity.getExp()
    }
    
    companion object {
        
        val INTERACTIVE = TileEntityBlock(true)
        val NON_INTERACTIVE = TileEntityBlock(false)
        
    }
    
}