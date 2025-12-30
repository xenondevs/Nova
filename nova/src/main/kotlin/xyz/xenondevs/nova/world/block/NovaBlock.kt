@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package xyz.xenondevs.nova.world.block

import com.mojang.serialization.Codec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.serialization.kotlinx.NovaBlockSerializer
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorFactory
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.toNms
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.InteractionResult as NmsInteractionResult
import net.minecraft.world.entity.player.Player as NmsPlayer
import net.minecraft.world.item.ItemStack as NmsItemStack

/**
 * Represents a custom Nova block type.
 */
@Serializable(with = NovaBlockSerializer::class)
open class NovaBlock internal constructor(
    val id: Key,
    val name: Component,
    val style: Style,
    behaviors: List<BlockBehaviorHolder>,
    val stateProperties: List<ScopedBlockStateProperty<*>>,
    configId: String,
    internal val layout: BlockModelLayout
) {
    
    /**
     * The [NovaItem] associated with this [NovaBlock].
     */
    var item: NovaItem? = null
        internal set
    
    /**
     * The configuration for this [NovaBlock].
     * May be an empty node if the config file does not exist.
     */
    val config: Provider<CommentedConfigurationNode> = Configs[configId]
    
    /**
     * A list of all [BlockBehaviors][BlockBehavior] of this [NovaBlock].
     */
    val behaviors: List<BlockBehavior> = behaviors.map { holder ->
        when (holder) {
            is BlockBehavior -> holder
            is BlockBehaviorFactory<*> -> holder.create(this)
        }
    }
    
    /**
     * A list of all possible [NovaBLockStates][NovaBlockState] of this [NovaBlock]
     */
    @Suppress("LeakingThis")
    val blockStates = NovaBlockState.createBlockStates(this, stateProperties)
    
    /**
     * The default block state of this [NovaBlock].
     */
    val defaultBlockState = blockStates[0]
    
    /**
     * Checks whether this [NovaBlock] has a [BlockBehavior] of the reified type [T], or a subclass of it.
     */
    inline fun <reified T : Any> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    /**
     * Checks whether this [NovaBlock] has a [BlockBehavior] of the specified class [type], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: KClass<T>): Boolean =
        behaviors.any { type.isSuperclassOf(it::class) }
    
    /**
     * Checks whether this [NovaBlock] has a [BlockBehavior] of the specified class [type], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: Class<T>): Boolean =
        behaviors.any { type.isAssignableFrom(it::class.java) }
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [T], or null if there is none.
     */
    inline fun <reified T : Any> getBehaviorOrNull(): T? =
        getBehaviorOrNull(T::class)
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [type] or a subclass, or null if there is none.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getBehaviorOrNull(type: KClass<T>): T? =
        behaviors.firstOrNull { type.isSuperclassOf(it::class) } as T?
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [type] or a subclass, or null if there is none.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getBehaviorOrNull(type: Class<T>): T? =
        behaviors.firstOrNull { type.isAssignableFrom(it::class.java) } as T?
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [T], or throws an [IllegalStateException] if there is none.
     */
    inline fun <reified T : Any> getBehavior(): T =
        getBehavior(T::class)
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [type], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(type: KClass<T>): T =
        getBehaviorOrNull(type) ?: throw IllegalStateException("Block $id does not have a behavior of type ${type.simpleName}")
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [type], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(type: Class<T>): T =
        getBehaviorOrNull(type) ?: throw IllegalStateException("Block $id does not have a behavior of type ${type.simpleName}")
    
    //<editor-fold desc="event methods">
    /**
     * Checks whether a block of [state] can be placed at [pos] using the given [ctx].
     */
    suspend fun canPlace(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockPlace>
    ): Boolean = coroutineScope {
        if (behaviors.isEmpty())
            return@coroutineScope true
        
        return@coroutineScope behaviors
            .map { async { it.canPlace(pos, state, ctx) } }
            .awaitAll()
            .all { it }
    }
    
    /**
     * Chooses the appropriate [NovaBlockState] for placement given the [ctx].
     */
    fun chooseBlockState(ctx: Context<BlockPlace>): NovaBlockState {
        return defaultBlockState.tree?.get(ctx) ?: defaultBlockState
    }
    
    internal fun useItemOnNms(
        pos: BlockPos,
        state: NovaBlockState,
        nmsItemStack: NmsItemStack,
        nmsPlayer: NmsPlayer,
        nmsHand: InteractionHand,
        hitResult: BlockHitResult,
    ): NmsInteractionResult {
        // check cooldown since Nova applies cooldowns in all item-use cases
        if (nmsPlayer.cooldowns.isOnCooldown(nmsItemStack))
            return NmsInteractionResult.PASS
        
        val player = nmsPlayer.bukkitEntity
        val itemStack = nmsItemStack.asBukkitCopy()
        val hand = nmsHand.bukkitEquipmentSlot
        val face = hitResult.direction.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, itemStack, pos))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK_POS, pos)
            .param(BlockInteract.BLOCK_STATE_NOVA, state)
            .param(BlockInteract.SOURCE_ENTITY, player)
            .param(BlockInteract.HELD_ITEM_STACK, itemStack)
            .param(BlockInteract.HELD_HAND, hand)
            .param(BlockInteract.CLICKED_BLOCK_FACE, face)
            .build()
        
        val result = useItemOn(pos, state, ctx)
        if (result is InteractionResult.Success)
            result.performActions(player, hand)
        
        return when (val nms = result.toNms()) {
            is NmsInteractionResult.Pass -> NmsInteractionResult.TRY_WITH_EMPTY_HAND
            else -> nms
        }
    }
    
    /**
     * Uses an item on the block of [state] at [pos].
     */
    fun useItemOn(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockInteract>
    ): InteractionResult = runSafely("use item on", InteractionResult.Fail) {
        for (behavior in behaviors) {
            val result = behavior.useItemOn(pos, state, ctx)
            if (result !is InteractionResult.Pass)
                return result
        }
        return InteractionResult.Pass
    }
    
    internal fun useNms(
        pos: BlockPos,
        state: NovaBlockState,
        nmsPlayer: NmsPlayer,
        hitResult: BlockHitResult,
    ): NmsInteractionResult {
        val player = nmsPlayer.bukkitEntity
        val face = hitResult.direction.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, null, pos))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK_POS, pos)
            .param(BlockInteract.BLOCK_STATE_NOVA, state)
            .param(BlockInteract.SOURCE_ENTITY, player)
            .param(BlockInteract.CLICKED_BLOCK_FACE, face)
            .build()
        
        val result = use(pos, state, ctx)
        if (result is InteractionResult.Success) {
            require(!result.wasItemInteraction) { "useWithoutItem cannot result in an item interaction" }
            result.performActions(player, EquipmentSlot.HAND)
        }
        return result.toNms()
    }
    
    /**
     * Uses the block of [state] at [pos] by itself, without using an item.
     */
    fun use(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockInteract>
    ): InteractionResult = runSafely("use", InteractionResult.Fail) {
        for (behavior in behaviors) {
            val result = behavior.use(pos, state, ctx)
            if (result !is InteractionResult.Pass)
                return result
        }
        return InteractionResult.Pass
    }
    
    /**
     * Handles attack (left-click) on a block of [state] at [pos] with the given [ctx].
     */
    fun handleAttack(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): Unit = runSafely("handle attack") {
        behaviors.forEach { it.handleAttack(pos, state, ctx) }
    }
    
    /**
     * Handles the placement of a block of [state] at [pos] with the given [ctx].
     */
    open fun handlePlace(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockPlace>
    ): Unit = runSafely("handle place") {
        state.modelProvider.set(pos)
        behaviors.forEach { it.handlePlace(pos, state, ctx) }
    }
    
    /**
     * Handles the destruction of a block of [state] at [pos] with the given [ctx].
     */
    open fun handleBreak(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): Unit = runSafely("handle break") {
        state.modelProvider.remove(pos)
        if (state[DefaultBlockStateProperties.WATERLOGGED] == true)
            pos.block.type = Material.WATER
        behaviors.forEach { it.handleBreak(pos, state, ctx) }
    }
    
    /**
     * Called when a redstone update happened that may affect this [state] at [pos].
     */
    fun handleNeighborChanged(
        pos: BlockPos,
        state: NovaBlockState
    ): Unit = runSafely("handle neighbor changed") {
        behaviors.forEach { it.handleNeighborChanged(pos, state) }
    }
    
    /**
     * Called when a block at [neighborPos] changed to update the [NovaBlockState] of this [state] at [pos].
     */
    fun updateShape(
        pos: BlockPos,
        state: NovaBlockState,
        neighborPos: BlockPos
    ): NovaBlockState = runSafely("update shape", state) {
        return behaviors.fold(state) { acc, behavior -> behavior.updateShape(pos, acc, neighborPos) }
    }
    
    /**
     * Handles a random tick for a block of [state] at [pos].
     */
    fun handleRandomTick(
        pos: BlockPos,
        state: NovaBlockState
    ): Unit = runSafely("handle random tick") {
        behaviors.forEach { it.handleRandomTick(pos, state) }
    }
    
    /**
     * Handles a scheduled tick for a block of [state] at [pos].
     */
    fun handleScheduledTick(
        pos: BlockPos,
        state: NovaBlockState
    ): Unit = runSafely("handle scheduled tick") {
        behaviors.forEach { it.handleScheduledTick(pos, state) }
    }
    
    /**
     * Called when an [entity] is inside a block of [state] at [pos].
     */
    fun handleEntityInside(
        pos: BlockPos,
        state: NovaBlockState,
        entity: Entity
    ): Unit = runSafely("handle entity inside") {
        return behaviors.forEach { it.handleEntityInside(pos, state, entity) }
    }
    
    /**
     * Retrieves the items that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getDrops(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): List<ItemStack> = runSafely("get drops", emptyList()) {
        return behaviors.flatMap { it.getDrops(pos, state, ctx) }
    }
    
    /**
     * Retrieves the amount of experience that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getExp(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): Int = runSafely("get exp", 0) {
        return behaviors.sumOf { it.getExp(pos, state, ctx) }
    }
    
    /**
     * Chooses the [ItemStack] that should be given to the player when mid-clicking a block of [state] at [pos] with the given [ctx] in creative mode.
     */
    fun pickBlockCreative(
        pos: BlockPos,
        state: NovaBlockState,
        ctx: Context<BlockInteract>
    ): ItemStack? = runSafely("pick block creative", item?.createItemStack()) {
        return behaviors.firstNotNullOfOrNull { it.pickBlockCreative(pos, state, ctx) } ?: item?.createItemStack()
    }
    
    private inline fun runSafely(name: String, run: () -> Unit) = runSafely(name, Unit, run)
    
    private inline fun <T> runSafely(name: String, fallback: T, run: () -> T): T {
        checkServerThread()
        try {
            return run()
        } catch (t: Throwable) {
            LOGGER.error("Failed to $name for $id", t)
        }
        return fallback
    }
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC: Codec<NovaBlock> = NovaRegistries.BLOCK.byNameCodec()
        
    }
    
}