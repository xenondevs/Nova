package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState

/**
 * Supertype for everything that is or can provide a [BlockBehavior].
 */
sealed interface BlockBehaviorHolder

/**
 * For handling block logic.
 */
interface BlockBehavior : BlockBehaviorHolder {
    
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
    
}

/**
 * Factory for creating [BlockBehavior] instances of [T] based on a [NovaBlock].
 */
interface BlockBehaviorFactory<T : BlockBehavior> : BlockBehaviorHolder {
    
    /**
     *  Creates a new [BlockBehavior] instance of [T] based on the given [block].
     */
    fun create(block: NovaBlock): T

}