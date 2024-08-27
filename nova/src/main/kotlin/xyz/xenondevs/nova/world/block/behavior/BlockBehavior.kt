package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockPlace
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
     * Returns whether an interaction has taken place. If an interaction has taken place,
     * subsequent behaviors will not be called.
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
     * Called when a block at [neighborPos] changed to update the [NovaBlockState] of this [state] at [pos].
     */
    fun updateShape(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos): NovaBlockState = state
    
    /**
     * Called when an [entity] is inside a block of [state] at [pos].
     */
    fun handleEntityInside(pos: BlockPos, state: NovaBlockState, entity: Entity) = Unit
    
    /**
     * Whether this behavior implements random-tick logic for the given [state].
     * Note that the result of this method will be cached on startup.
     */
    fun ticksRandomly(state: NovaBlockState): Boolean = false
    
    /**
     * Handles a random tick for a block of [state] at [pos].
     */
    fun handleRandomTick(pos: BlockPos, state: NovaBlockState) = Unit
    
    /**
     * Handles a scheduled tick for a block of [state] at [pos].
     */
    fun handleScheduledTick(pos: BlockPos, state: NovaBlockState) = Unit // TODO: implement scheduled ticks via WorldDataManager
    
    /**
     * Retrieves the amount of experience that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int = 0
    
    /**
     * Retrieves the items that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> = emptyList()
    
    /**
     * Chooses the [ItemStack] that should be given to the player when mid-clicking a block of [state] at [pos] with the given [ctx] in creative mode.
     */
    fun pickBlockCreative(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): ItemStack? = null
    
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