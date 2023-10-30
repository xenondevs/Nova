package xyz.xenondevs.nova.world.block

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

interface BlockBehavior<T : NovaBlockState> {
    
    fun handleInteract(state: T, ctx: Context<BlockInteract>): Boolean
    fun handlePlace(state: T, ctx: Context<BlockPlace>)
    fun handleBreak(state: T, ctx: Context<BlockBreak>)
    fun getDrops(state: T, ctx: Context<BlockBreak>): List<ItemStack>
    fun getExp(state: T, ctx: Context<BlockBreak>): Int
    
    open class Default<T : NovaBlockState> protected constructor() : BlockBehavior<T> {
        
        override fun handleInteract(state: T, ctx: Context<BlockInteract>): Boolean = false
        override fun handlePlace(state: T, ctx: Context<BlockPlace>) = Unit
        override fun handleBreak(state: T, ctx: Context<BlockBreak>) = Unit
        override fun getExp(state: T, ctx: Context<BlockBreak>): Int = 0
        
        override fun getDrops(state: T, ctx: Context<BlockBreak>): List<ItemStack> {
            if ((ctx[ContextParamTypes.SOURCE_ENTITY] as? Player)?.gameMode == GameMode.CREATIVE)
                return emptyList()
            
            val item = state.block.item ?: return emptyList()
            
            return listOf(item.createItemStack())
        }
        
        companion object : Default<NovaBlockState>()
        
    }
    
}