package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState

interface BlockBehavior {
    
    /**
     * Checks whether a block of [state] can be placed at [pos] using the given [ctx].
     * 
     * Should only suspend for [ProtectionManager] checks, and it is assumed that this function does not suspend
     * when the source is online.
     */
    suspend fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>): Boolean = true
    
    /**
     * Handles interaction (right-click) with a block of [state] at [pos] with the given [ctx].
     *
     * Returns whether an interaction has taken place.
     */
    fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean = false
    
    /**
     * Handles attack (left-click) on a block of [state] at [pos] with the given [ctx].
     */
    fun handleAttack(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) = Unit
    
    /**
     * Handles the placement of a block of [state] at [pos] with the given [ctx].
     */
    fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) = Unit
    
    /**
     * Handles the destruction of a block of [state] at [pos] with the given [ctx].
     */
    fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) = Unit
    
    /**
     * Called when a block at [neighborPos] changed next to this [state] at [pos].
     */
    fun handleNeighborChanged(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos) = Unit
    
    /**
     * Handles a random tick for a block of [state] at [pos].
     */
    fun handleRandomTick(pos: BlockPos, state: NovaBlockState) = Unit
    
    /**
     * Retrieves the amount of experience that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int = 0
    
    /**
     * Retrieves the items that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> = emptyList()
    
    /**
     * The default block behavior.
     */
    open class Default : BlockBehavior {
        
        companion object : Default()
        
        override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
            if ((ctx[ContextParamTypes.SOURCE_ENTITY] as? Player)?.gameMode == GameMode.CREATIVE)
                return emptyList()
            
            return state.block.item
                ?.let { listOf(it.createItemStack()) }
                ?: return emptyList()
        }
        
    }
    
}