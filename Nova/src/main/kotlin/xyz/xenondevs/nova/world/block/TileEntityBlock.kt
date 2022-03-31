package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext

open class TileEntityBlock protected constructor(private val interactable: Boolean) : NovaBlock.Default<NovaTileEntityState>() {
    
    override fun handleInteract(state: NovaTileEntityState, ctx: BlockInteractContext): Boolean =
        if (interactable) state.tileEntity.handleRightClick(ctx) else false
    
    override fun getDrops(state: NovaTileEntityState, ctx: BlockBreakContext): List<ItemStack> {
        return state.tileEntity.getDrops(ctx.source !is Player || ctx.source.gameMode != GameMode.CREATIVE)
    }
    
    companion object {
        
        val INTERACTIVE = TileEntityBlock(true)
        val NON_INTERACTIVE = TileEntityBlock(false)
        
    }
    
}