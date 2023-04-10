package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

interface BlockBehavior<T : NovaBlockState> {
    
    fun handleInteract(state: T, ctx: BlockInteractContext): Boolean
    fun handlePlace(state: T, ctx: BlockPlaceContext)
    fun handleBreak(state: T, ctx: BlockBreakContext)
    fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack>
    fun getExp(state: T, ctx: BlockBreakContext): Int
    
    open class Default<T : NovaBlockState> protected constructor() : BlockBehavior<T> {
        
        override fun handleInteract(state: T, ctx: BlockInteractContext) = false
        override fun handlePlace(state: T, ctx: BlockPlaceContext) = Unit
        override fun handleBreak(state: T, ctx: BlockBreakContext) = Unit
        override fun getExp(state: T, ctx: BlockBreakContext) = 0
        
        override fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack> {
            if (ctx.source is Player && ctx.source.gameMode == GameMode.CREATIVE)
                return emptyList()
            
            val item = state.block.item ?: return emptyList()
            
            return listOf(item.createItemStack())
        }
        
        companion object : Default<NovaBlockState>()
        
    }
    
}