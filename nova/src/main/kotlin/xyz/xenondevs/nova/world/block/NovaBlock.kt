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
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.serialization.kotlinx.NovaBlockSerializer
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorFactory
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.item.NovaItem
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Represents a block type in Nova.
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
    suspend fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>): Boolean = coroutineScope {
        if (behaviors.isEmpty())
            return@coroutineScope true
        
        return@coroutineScope behaviors
            .map { async { it.canPlace(pos, state, ctx) } }
            .awaitAll()
            .all { it }
    }
    
    fun chooseBlockState(ctx: Context<BlockPlace>): NovaBlockState {
        return defaultBlockState.tree?.get(ctx) ?: defaultBlockState
    }
    
    fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        checkServerThread()
        for (behavior in behaviors) {
            if (behavior.handleInteract(pos, state, ctx))
                return true
        }
        return false
    }
    
    fun handleAttack(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        checkServerThread()
        behaviors.forEach { it.handleAttack(pos, state, ctx) }
    }
    
    open fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        checkServerThread()
        state.modelProvider.set(pos)
        behaviors.forEach { it.handlePlace(pos, state, ctx) }
    }
    
    open fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        checkServerThread()
        state.modelProvider.remove(pos)
        if (state[DefaultBlockStateProperties.WATERLOGGED] == true)
            pos.block.type = Material.WATER
        behaviors.forEach { it.handleBreak(pos, state, ctx) }
    }
    
    fun handleNeighborChanged(pos: BlockPos, state: NovaBlockState) {
        checkServerThread()
        behaviors.forEach { it.handleNeighborChanged(pos, state) }
    }
    
    fun updateShape(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos): NovaBlockState {
        checkServerThread()
        return behaviors.fold(state) { acc, behavior -> behavior.updateShape(pos, acc, neighborPos) }
    }
    
    fun handleRandomTick(pos: BlockPos, state: NovaBlockState) {
        checkServerThread()
        behaviors.forEach { it.handleRandomTick(pos, state) }
    }
    
    fun handleScheduledTick(pos: BlockPos, state: NovaBlockState) {
        checkServerThread()
        behaviors.forEach { it.handleScheduledTick(pos, state) }
    }
    
    fun handleEntityInside(pos: BlockPos, state: NovaBlockState, entity: Entity) {
        checkServerThread()
        return behaviors.forEach { it.handleEntityInside(pos, state, entity) }
    }
    
    fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        checkServerThread()
        return behaviors.flatMap { it.getDrops(pos, state, ctx) }
    }
    
    fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int {
        checkServerThread()
        return behaviors.sumOf { it.getExp(pos, state, ctx) }
    }
    
    fun pickBlockCreative(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): ItemStack? {
        return behaviors.firstNotNullOfOrNull { it.pickBlockCreative(pos, state, ctx) } ?: item?.createItemStack()
    }
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC: Codec<NovaBlock> = NovaRegistries.BLOCK.byNameCodec()
        
    }
    
}