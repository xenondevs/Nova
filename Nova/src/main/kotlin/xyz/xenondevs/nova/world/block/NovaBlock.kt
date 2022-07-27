package xyz.xenondevs.nova.world.block

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.showBreakParticles
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

abstract class NovaBlock<T : NovaBlockState> {
    
    abstract fun handleInteract(state: T, ctx: BlockInteractContext): Boolean
    
    abstract fun handlePlace(state: T, ctx: BlockPlaceContext)
    
    abstract fun handleBreak(state: T, ctx: BlockBreakContext)
    
    abstract fun playBreakSound(state: T, ctx: BlockBreakContext)
    
    abstract fun showBreakParticles(state: T, ctx: BlockBreakContext)
    
    abstract fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack>
    
    open class Default<T : NovaBlockState> : NovaBlock<T>() {
        
        override fun handleInteract(state: T, ctx: BlockInteractContext) = false
        override fun handlePlace(state: T, ctx: BlockPlaceContext) = Unit
        override fun handleBreak(state: T, ctx: BlockBreakContext) = Unit
        override fun getDrops(state: T, ctx: BlockBreakContext) = emptyList<ItemStack>()
        
        override fun playBreakSound(state: T, ctx: BlockBreakContext) {
            state.material.breakSound?.play(ctx.pos)
        }
        
        override fun showBreakParticles(state: T, ctx: BlockBreakContext) {
            state.material.breakParticles?.showBreakParticles(ctx.pos.location)
        }
        
    }
    
}