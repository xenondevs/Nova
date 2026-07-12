package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty

/**
 * Supertype for everything that is or can provide a [BlockBehavior].
 */
sealed interface BlockBehaviorHolder

/**
 * For handling block logic.
 */
interface BlockBehavior : BlockBehaviorHolder {
    
    /**
     * The [BlockStateProperties][BlockStateProperty] that are required by this behavior
     * and will be automatically added to all blocks that implement this behavior.
     */
    val stateProperties: Set<BlockStateProperty<*>>
        get() = emptySet()
    
    /**
     * Checks whether a block of [data] can be placed at [block] using the given [ctx].
     *
     * Should only suspend for [ProtectionManager] checks, and it is assumed that this function does not suspend
     * when the source is online.
     */
    suspend fun canPlace(block: Block, data: NovaBlockState, ctx: Context<BlockPlace>): Boolean = true
    
    /**
     * Uses the block of [state] at [block] by itself, without using an item.
     * 
     * This function is only called if all [useItemOn] calls return [InteractionResult.Pass].
     * 
     * Returning a result with [InteractionResult.Success.wasItemInteraction] not allowed.
     * For that, use [useItemOn] instead.
     */
    fun use(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult = InteractionResult.Pass
    
    /**
     * Uses an item on the block of [state] at [block].
     * 
     * If all behaviors return [InteractionResult.Pass], [use] will be called.
     */
    fun useItemOn(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult = InteractionResult.Pass
    
    /**
     * Handles attack (left-click) on a block of [state] at [block] with the given [ctx].
     */
    fun handleAttack(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>) = Unit
    
    /**
     * Handles the placement of a block of [state] at [block] with the given [ctx].
     */
    fun handlePlace(block: Block, state: NovaBlockState, ctx: Context<BlockPlace>) = Unit
    
    /**
     * Handles the destruction of a block of [state] at [block] with the given [ctx].
     */
    fun handleBreak(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>) = Unit
    
    /**
     * Called when a redstone update happened that may affect this [state] at [block].
     */
    fun handleNeighborChanged(block: Block, state: NovaBlockState) = Unit
    
    /**
     * Called when a block at [neighbor] changed to update the [NovaBlockState] of this [state] at [block].
     */
    fun updateShape(block: Block, state: NovaBlockState, neighbor: Block, neighborState: BlockData): NovaBlockState = state
    
    /**
     * Called when an [entity] is inside a block of [state] at [block].
     */
    fun handleEntityInside(block: Block, state: NovaBlockState, entity: Entity) = Unit
    
    /**
     * Whether this behavior implements random-tick logic for the given [state].
     * Note that the result of this method will be cached on startup.
     */
    fun ticksRandomly(state: NovaBlockState): Boolean = false
    
    /**
     * Handles a random tick for a block of [state] at [block].
     */
    fun handleRandomTick(block: Block, state: NovaBlockState) = Unit
    
    /**
     * Handles a scheduled tick for a block of [state] at [block].
     */
    fun handleScheduledTick(block: Block, state: NovaBlockState) = Unit // TODO: implement triggering scheduled ticks
    
    /**
     * Retrieves the amount of experience that would be dropped when breaking a block of [state] at [block] with the given [ctx].
     * Handlers should check [BlockBreak.BLOCK_EXP_DROPS].
     */
    fun getExp(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>): Int = 0
    
    /**
     * Retrieves the items that would be dropped when breaking a block of [state] at [block] with the given [ctx].
     * Handlers should check [BlockBreak.BLOCK_DROPS] and [BlockBreak.BLOCK_STORAGE_DROPS].
     */
    fun getDrops(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> = emptyList()
    
    /**
     * Chooses the [ItemStack] that should be given to the player when mid-clicking a block of [state] at [block] with the given [ctx] in creative mode.
     * @see BlockInteract.INCLUDE_DATA
     */
    fun pickBlockCreative(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): ItemStack? = null
    
}

/**
 * Factory for creating [BlockBehavior] instances of [T] based on a [NovaBlock].
 */
fun interface BlockBehaviorFactory<T : BlockBehavior> : BlockBehaviorHolder {
    
    /**
     * Creates a new [BlockBehavior] for [entry] configured by [config].
     */
    fun create(entry: RegistryEntry.Paper<BlockType>, config: ConfigProvider): T
    
}
