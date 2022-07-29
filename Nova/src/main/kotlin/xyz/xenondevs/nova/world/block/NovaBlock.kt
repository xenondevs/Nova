package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.showBreakParticles
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

interface NovaBlock<T : NovaBlockState> {
    
    fun handleInteract(state: T, ctx: BlockInteractContext): Boolean
    fun handlePlace(state: T, ctx: BlockPlaceContext)
    fun handleBreak(state: T, ctx: BlockBreakContext)
    fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack>
    fun playBreakSound(state: T, ctx: BlockBreakContext)
    fun showBreakParticles(state: T, ctx: BlockBreakContext)
    
    open class Default<T : NovaBlockState> protected constructor() : NovaBlock<T> {
        
        override fun handleInteract(state: T, ctx: BlockInteractContext) = false
        override fun handlePlace(state: T, ctx: BlockPlaceContext) = Unit
        override fun handleBreak(state: T, ctx: BlockBreakContext) = Unit
        
        override fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack> {
            if (ctx.source is Player && ctx.source.gameMode == GameMode.CREATIVE)
                return emptyList()
            
            return listOf(state.material.createItemStack(1))
        }
        
        override fun playBreakSound(state: T, ctx: BlockBreakContext) {
            state.material.breakSound?.play(ctx.pos)
        }
        
        override fun showBreakParticles(state: T, ctx: BlockBreakContext) {
            state.material.breakParticles?.showBreakParticles(ctx.pos.location)
        }
        
        companion object : Default<NovaBlockState>()
        
    }
    
}