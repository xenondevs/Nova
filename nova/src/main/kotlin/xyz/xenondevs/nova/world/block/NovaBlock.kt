package xyz.xenondevs.nova.world.block

import io.papermc.paper.registry.RegistryKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.InsideBlockEffectApplier
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.bukkit.Chunk
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.block.CraftBlockType
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.context.intention.ImplicitIntentions
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.network.currentPacketSourcePlayer
import xyz.xenondevs.nova.registry.FlammableSettings
import xyz.xenondevs.nova.registry.ProtoBlockState
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.bootstrapFlatMap
import xyz.xenondevs.nova.resources.builder.layout.block.BlockSelectorScope
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.nmsBlockEntity
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.toBlock
import xyz.xenondevs.nova.util.toPropertyStringMap
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.model.BlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.item.createItemStack
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.world.toNms
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.core.BlockPos as NmsBlockPos
import net.minecraft.world.InteractionResult as NmsInteractionResult
import net.minecraft.world.entity.Entity as NmsEntity
import net.minecraft.world.entity.player.Player as NmsPlayer
import net.minecraft.world.item.ItemStack as NmsItemStack
import net.minecraft.world.level.block.state.BlockState as NmsBlockState
import org.bukkit.block.Block as BukkitBlock
import org.bukkit.block.BlockState as CapturedBlockState
import org.bukkit.craftbukkit.block.CraftBlockState as CraftCapturedBlockState

private val BLOCK_STATE_CACHED_TYPE: VarHandle = MethodHandles
    .privateLookupIn(NmsBlockState::class.java, MethodHandles.lookup())
    .findVarHandle(NmsBlockState::class.java, $$"nova$cachedType", BlockType::class.java)

private val BLOCK_STATE_CACHED_TYPE_ENTRY: VarHandle = MethodHandles
    .privateLookupIn(NmsBlockState::class.java, MethodHandles.lookup())
    .findVarHandle(NmsBlockState::class.java, $$"nova$cachedTypeEntry", Any::class.java)

private val NmsBlockState.blockType: BlockType
    get() {
        val cached = BLOCK_STATE_CACHED_TYPE.get(this)
        if (cached != null)
            return cached as BlockType
        val blockType = CraftBlockType.minecraftToBukkitNew(block)
        BLOCK_STATE_CACHED_TYPE.set(this, blockType)
        return blockType
    }

@Suppress("UNCHECKED_CAST")
private val NmsBlockState.blockTypeEntry: RegistryEntry.Paper<BlockType>
    get() {
        val cached = BLOCK_STATE_CACHED_TYPE.get(this)
        if (cached != null)
            return BLOCK_STATE_CACHED_TYPE_ENTRY.get(this) as RegistryEntry.Paper<BlockType>
        val entry = RegistryEntry.paper(RegistryKey.BLOCK, blockType)
        BLOCK_STATE_CACHED_TYPE_ENTRY.set(this, entry)
        return entry
    }

internal val BlockData.clientsideBlockState: BlockData
    get() = (this as? NovaBlockState)
        ?.novaBlock
        ?.clientsideBlockStates
        ?.get(nmsBlockState)
        ?.bukkitBlockData
        ?: this

@PublishedApi
internal val BlockType.novaBlock: NovaBlock?
    get() = (this as CraftBlockType<*>).handle as? NovaBlock

@PublishedApi
internal fun <T : Any> BlockType.hasBehavior(type: KClass<T>): Boolean =
    novaBlock?.behaviors?.any { type.isSuperclassOf(it::class) } == true

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> BlockType.getBehaviorOrNull(type: KClass<T>): T? =
    novaBlock?.behaviors?.firstOrNull { type.isSuperclassOf(it::class) } as T?

@PublishedApi
internal fun <T : Any> BlockType.getBehaviorOrThrow(type: KClass<T>): T =
    getBehaviorOrNull(type) ?: throw NoSuchElementException("${key.asString()} has no behavior of type ${type.simpleName}")

var BukkitBlock.blockType: BlockType
    get() = (this as CraftBlock).blockState.blockType
    set(value) {
        blockData = value.createBlockData()
    }

val BukkitBlock.clientsideBlockState: BlockData
    get() {
        val data = blockData
        if (data is NovaBlockState) {
            return data.novaBlock.clientsideBlockStates[data.nmsBlockState]?.bukkitBlockData ?: data
        } else {
            return data
        }
    }

val BukkitBlock.blockTypeEntry: RegistryEntry.Paper<BlockType>
    get() = (this as CraftBlock).blockState.blockTypeEntry

var BukkitBlock.novaBlockState: NovaBlockState?
    get() = blockData as? NovaBlockState
    set(value) {
        blockData = value ?: BlockType.AIR.createBlockData()
    }

val CapturedBlockState.blockType: BlockType
    get() = (this as CraftCapturedBlockState).block.blockType

val BlockType.isNova: Boolean
    get() = novaBlock != null

val BlockType.isNovaTileEntity: Boolean
    get() = novaBlock is NovaTileEntityBlock

val BlockType.name: Component
    get() = novaBlock?.name ?: Component.translatable(nmsBlock.descriptionId)

inline fun <reified T : Any> BlockType.hasBehavior(): Boolean =
    hasBehavior(T::class)

inline fun <reified T : Any> BlockType.getBehaviorOrNull(): T? =
    getBehaviorOrNull(T::class)

inline fun <reified T : Any> BlockType.getBehaviorOrThrow(): T =
    getBehaviorOrThrow(T::class)

val BlockData.blockType: BlockType
    get() = (this as CraftBlockData).state.blockType

val BukkitBlock.novaTileEntity: TileEntity?
    get() = (nmsBlockEntity as? NovaTileEntityProxy)?.tileEntity

val BlockType.itemTypeOrNull: ItemType?
    get() = if (hasItemType()) itemType else null

val ItemType.blockTypeOrNull: BlockType?
    get() = if (hasBlockType()) blockType else null

@Suppress("UNCHECKED_CAST")
private fun <T : Comparable<T>> NmsBlockState.setValue(property: BlockStateProperty<T>, value: Any): NmsBlockState =
    setValue(property.nmsProperty, value as T)

/**
 * Shortcut for `bootstrapFlatMap { it.config }` 
 */
val Provider<BlockType>.config: Provider<ConfigProvider>
    get() = bootstrapFlatMap { it.config }

/**
 * Gets the type's config if [BlockType.isNova], otherwise [ConfigProvider.Empty].
 */
val BlockType.config: Provider<ConfigProvider>
    get() = novaBlock?.config ?: provider(ConfigProvider.Empty)

/**
 * Returns a snapshot of all nova tile entities in this chunk.
 */
val Chunk.novaTileEntities: List<TileEntity>
    get() = levelChunk.blockEntities.values.mapNotNull { (it as? NovaTileEntityProxy)?.tileEntity }

internal open class NovaBlock(
    val entry: RegistryEntry.Paper<BlockType>,
    name: Provider<Component>,
    style: Provider<Style>,
    behaviors: Provider<List<BlockBehavior>>,
    val stateProperties: List<BlockStateProperty<*>>,
    item: Provider<RegistryEntry.Paper<ItemType>?>,
    val config: Provider<ConfigProvider>,
    properties: Provider<Properties>,
    flammable: Provider<FlammableSettings>,
    selectFluidFlowMode: Provider<BlockSelectorScope.() -> FluidFlowMode>,
    breakParticles: Provider<ItemType?>,
    showBreakAnimation: Provider<Boolean>,
    soundGroup: Provider<SoundGroup?>
) : Block(properties.get()) {
    
    val key: Key
        get() = entry.key
    
    val name by combinedProvider(name, style) { name, style -> name.style(style) }
    val style by style
    val behaviors by behaviors
    val item by item
    val breakParticles by breakParticles
    val showBreakAnimation by showBreakAnimation
    val soundGroup by soundGroup
    
    val fluidFlowModes: Map<NmsBlockState, FluidFlowMode>
        by selectFluidFlowMode.map { selector ->
            stateDefinition.possibleStates.associateWithTo(IdentityHashMap()) { state ->
                val proto = ProtoBlockState(entry, state.toPropertyStringMap())
                val mode = BlockSelectorScope(proto).selector()
                if (mode.requiresWaterloggedState && !state.hasProperty(BlockStateProperties.WATERLOGGED))
                    FluidFlowMode.BLOCK
                else mode
            }
        }
    
    val modelProviders: Provider<Map<NmsBlockState, BlockModelProvider>> = ResourceLookups.blockModelLookup.map { lookup ->
        lookup.entries
            .filter { [protoState, _] -> protoState.entry == entry }
            .associateTo(IdentityHashMap()) { [protoState, model] -> protoState.toBlockState(defaultBlockState) to model }
    }
    
    private val extraColliderShapes: Provider<Map<NmsBlockState, VoxelShape>> = modelProviders.map { providers ->
        providers.mapValuesTo(IdentityHashMap()) { entry ->
            (entry.value as? DisplayEntityBlockModelProvider)
                ?.info
                ?.extraColliders
                .orEmpty()
                .map { collider ->
                    Shapes.box(
                        collider.minX, collider.minY, collider.minZ,
                        collider.maxX, collider.maxY, collider.maxZ
                    )
                }
                .let { shapes -> Shapes.or(Shapes.empty(), *shapes.toTypedArray()) }
        }
    }
    
    private val _clientsideBlockStates: Provider<Map<NmsBlockState, NmsBlockState>> =
        modelProviders.map { it.mapValues { [_, mp] -> mp.clientsideBlockState } }
    
    val clientsideBlockStates: Map<NmsBlockState, NmsBlockState> by _clientsideBlockStates
    val clientsideBlock: Block by _clientsideBlockStates.map { it.entries.first().value.block }
    
    init {
        // adjust defaultBlockState to the actual defaults of the properties
        registerDefaultState(stateProperties.fold(defaultBlockState) { state, prop -> state.setValue(prop, prop.defaultValue) })
        
        // Vanilla initializes these caches before Nova's blocks are registered.
        //stateDefinition.possibleStates.forEach { it.initCache() } fixme
        
        val flammable = flammable.get()
        (Blocks.FIRE as FireBlock).setFlammable(this, flammable.igniteOdds, flammable.burnOdds)
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, NmsBlockState>) {
        for (property in STATE_PROPERTIES.get()) {
            builder.add(property.nmsProperty)
        }
    }
    
    override fun getShape(state: NmsBlockState, level: BlockGetter, pos: NmsBlockPos, context: CollisionContext): VoxelShape =
        withExtraColliders(
            state,
            modelProviders.get()[state]!!.clientsideBlockState.getShape(level, pos, context)
        )
    
    override fun getCollisionShape(state: NmsBlockState, level: BlockGetter, pos: NmsBlockPos, context: CollisionContext): VoxelShape =
        withExtraColliders(
            state,
            modelProviders.get()[state]!!.clientsideBlockState.getCollisionShape(level, pos, context)
        )
    
    private fun withExtraColliders(state: NmsBlockState, shape: VoxelShape): VoxelShape {
        val extraColliderShape = extraColliderShapes.get()[state]!!
        return if (extraColliderShape.isEmpty) shape else Shapes.or(shape, extraColliderShape)
    }
    
    override fun getOcclusionShape(state: NmsBlockState): VoxelShape =
        modelProviders.get()[state]!!.clientsideBlockState.occlusionShape
    
    override fun getLightDampening(state: NmsBlockState): Int =
        modelProviders.get()[state]!!.clientsideBlockState.lightDampening
    
    override fun propagatesSkylightDown(state: NmsBlockState): Boolean =
        modelProviders.get()[state]!!.clientsideBlockState.propagatesSkylightDown()
    
    override fun getFluidState(state: NmsBlockState): FluidState {
        return if (state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false) == true)
            Fluids.WATER.getSource(false)
        else super.getFluidState(state)
    }
    
    
    //<editor-fold desc="event methods">
    /**
     * Checks whether a block of [state] can be placed at [pos] using the given [ctx].
     */
    suspend fun canPlace(
        block: BukkitBlock,
        state: NovaBlockState,
        ctx: Context<BlockPlace>
    ): Boolean = coroutineScope {
        if (behaviors.isEmpty())
            return@coroutineScope true
        
        return@coroutineScope behaviors
            .map { async { it.canPlace(block, state, ctx) } }
            .awaitAll()
            .all { it }
    }
    
    /**
     * Chooses the appropriate [NovaBlockState] for placement given the [ctx].
     */
    fun chooseBlockState(ctx: Context<BlockPlace>): NovaBlockState {
        var blockState = defaultBlockState
        
        for (property in stateProperties) {
            blockState = blockState.setValue(property, property.initializer(ctx))
        }
        
        return NovaBlockStateImpl(blockState)
    }
    
    override fun useItemOn(
        nmsItemStack: NmsItemStack,
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsPlayer: NmsPlayer,
        nmsHand: InteractionHand,
        nmsHitResult: BlockHitResult
    ): NmsInteractionResult {
        // check cooldown since Nova applies cooldowns in all item-use cases
        if (nmsPlayer.cooldowns.isOnCooldown(nmsItemStack))
            return NmsInteractionResult.PASS
        
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val player = nmsPlayer.bukkitEntity
        val itemStack = nmsItemStack.asBukkitCopy()
        val hand = nmsHand.bukkitEquipmentSlot
        val face = nmsHitResult.direction.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, itemStack, block))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK, block)
            .param(BlockInteract.BLOCK_STATE, blockState)
            .param(BlockInteract.SOURCE_ENTITY, player)
            .param(BlockInteract.HELD_ITEM_STACK, itemStack)
            .param(BlockInteract.HELD_HAND, hand)
            .param(BlockInteract.CLICKED_BLOCK_FACE, face)
            .build()
        
        val result = runSafely("use item on", InteractionResult.Fail) {
            for (behavior in behaviors) {
                val result = behavior.useItemOn(block, blockState, ctx)
                if (result !is InteractionResult.Pass)
                    return@runSafely result
            }
            return@runSafely InteractionResult.Pass
        }
        
        if (result is InteractionResult.Success)
            result.performActions(player, hand)
        
        return when (val nms = result.toNms()) {
            is NmsInteractionResult.Pass -> NmsInteractionResult.TRY_WITH_EMPTY_HAND
            else -> nms
        }
    }
    
    override fun useWithoutItem(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsPlayer: NmsPlayer,
        nmsHitResult: BlockHitResult
    ): NmsInteractionResult {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val player = nmsPlayer.bukkitEntity
        val face = nmsHitResult.direction.blockFace
        
        if (player is Player && !ProtectionManager.canUseBlock(player, null, block))
            return NmsInteractionResult.FAIL
        
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK, block)
            .param(BlockInteract.BLOCK_STATE, blockState)
            .param(BlockInteract.SOURCE_ENTITY, player)
            .param(BlockInteract.CLICKED_BLOCK_FACE, face)
            .build()
        
        val result = runSafely("use", InteractionResult.Fail) {
            for (behavior in behaviors) {
                val result = behavior.use(block, blockState, ctx)
                if (result is InteractionResult.Success && result.wasItemInteraction)
                    throw IllegalArgumentException("useWithoutItem cannot result in an item interaction")
                if (result !is InteractionResult.Pass)
                    return@runSafely result
            }
            return@runSafely InteractionResult.Pass
        }
        
        if (result is InteractionResult.Success)
            result.performActions(player, EquipmentSlot.HAND)
        
        return result.toNms()
    }
    
    override fun attack(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsPlayer: NmsPlayer
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = Context.intention(BlockBreak)
            .param(BlockBreak.BLOCK, block)
            .param(BlockBreak.BLOCK_STATE, blockState)
            .param(BlockBreak.SOURCE_ENTITY, nmsPlayer.bukkitEntity)
            .build()
        
        runSafely("handle attack") {
            behaviors.forEach { it.handleAttack(block, blockState, ctx) }
        }
    }
    
    fun nmsHandlePlace(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsOldBlockState: NmsBlockState
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = ImplicitIntentions.BLOCK_PLACE.getOrNull()
            ?: Context.intention(BlockPlace)
                .param(BlockPlace.BLOCK, block)
                .param(BlockPlace.BLOCK_STATE, blockState)
                .param(BlockPlace.PREVIOUS_BLOCK_STATE, nmsOldBlockState.bukkitBlockData)
                .build()
        
        handlePlace(block, blockState, ctx)
    }
    
    /**
     * Handles the placement of a block of [state] at [pos] with the given [ctx].
     */
    protected fun handlePlace(
        block: BukkitBlock,
        state: NovaBlockState,
        ctx: Context<BlockPlace>
    ): Unit = runSafely("handle place") {
        modelProviders.get()[state.nmsBlockState]?.load(block)
        behaviors.forEach { it.handlePlace(block, state, ctx) }
    }
    
    fun nmsHandleStateChange(
        nmsOldBlockState: NmsBlockState,
        nmsNewBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos
    ) {
        val providers = modelProviders.get()
        val oldProvider = providers[nmsOldBlockState] ?: return
        val newProvider = providers[nmsNewBlockState] ?: return
        newProvider.replace(nmsPos.toBlock(nmsLevel.world), oldProvider)
    }
    
    fun nmsHandleBreak(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsNewBlockState: NmsBlockState // TODO: expose this or not?
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = ImplicitIntentions.BLOCK_BREAK.getOrNull() // TODO: also look for block place implicit intention as that can cause replacement -> break
            ?: Context.intention(BlockBreak)
                .param(BlockBreak.BLOCK, block)
                .param(BlockBreak.BLOCK_STATE, blockState)
                .build()
        
        handleBreak(block, blockState, ctx)
    }
    
    /**
     * Handles the destruction of a block of [state] at [pos] with the given [ctx].
     */
    protected fun handleBreak(
        block: BukkitBlock,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): Unit = runSafely("handle break") {
        modelProviders.get()[state.nmsBlockState]?.unload(block)
        behaviors.forEach { it.handleBreak(block, state, ctx) }
    }
    
    override fun neighborChanged(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsBlock: Block,
        nmsOrientation: Orientation?,
        movedByPiston: Boolean
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        runSafely("handle neighbor changed") {
            behaviors.forEach { it.handleNeighborChanged(block, blockState) }
        }
    }
    
    override fun updateShape(
        nmsBlockState: NmsBlockState,
        nmsLevel: LevelReader,
        nmsTickAccess: ScheduledTickAccess,
        nmsPos: NmsBlockPos,
        nmsDirectionToNeighbour: Direction,
        nmsNeighbourPos: NmsBlockPos,
        nmsNeighbourState: NmsBlockState,
        nmsRandom: RandomSource
    ): NmsBlockState {
        if (nmsLevel !is ServerLevel)
            return nmsBlockState // TODO: Find a good way to handle non-server level (e.g. during some early world gen)
        
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState: NovaBlockState = NovaBlockStateImpl(nmsBlockState)
        val neighborBlock = nmsNeighbourPos.toBlock(nmsLevel.world)
        val neighborBlockState = nmsNeighbourState.bukkitBlockData
        return runSafely("update shape", blockState) {
            behaviors.fold(blockState) { acc, behavior -> behavior.updateShape(block, acc, neighborBlock, neighborBlockState) }
        }.nmsBlockState
    }
    
    override fun randomTick(
        nmsBlockState: NmsBlockState,
        nmsLevel: ServerLevel,
        nmsPos: NmsBlockPos,
        nmsRandom: RandomSource
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        runSafely("handle random tick") {
            behaviors.forEach { it.handleRandomTick(block, blockState) }
        }
    }
    
    override fun tick(
        nmsBlockState: NmsBlockState,
        nmsLevel: ServerLevel,
        nmsPos: NmsBlockPos,
        nmsRandom: RandomSource
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        runSafely("handle scheduled tick") {
            behaviors.forEach { it.handleScheduledTick(block, blockState) }
        }
    }
    
    override fun isRandomlyTicking(nmsBlockState: NmsBlockState): Boolean {
        val blockState = NovaBlockStateImpl(nmsBlockState)
        return behaviors.any { it.ticksRandomly(blockState) }
    }
    
    override fun entityInside(
        nmsBlockState: NmsBlockState,
        nmsLevel: Level,
        nmsPos: NmsBlockPos,
        nmsEntity: NmsEntity,
        nmsEffectApplier: InsideBlockEffectApplier,
        isPrecise: Boolean
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val entity = nmsEntity.bukkitEntity
        runSafely("handle entity inside") {
            return behaviors.forEach { it.handleEntityInside(block, blockState, entity) }
        }
    }
    
    /**
     * Retrieves the items that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getDrops(
        block: BukkitBlock,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): List<ItemStack> = runSafely("get drops", emptyList()) {
        return behaviors.flatMap { it.getDrops(block, state, ctx) }
    }
    
    override fun getExpDrop(
        nmsBlockState: NmsBlockState,
        nmsLevel: ServerLevel,
        nmsPos: NmsBlockPos,
        nmsTool: NmsItemStack,
        dropExperience: Boolean
    ): Int {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = ImplicitIntentions.BLOCK_BREAK.getOrNull()
            ?: Context.intention(BlockBreak)
                .param(BlockBreak.BLOCK, block)
                .param(BlockBreak.BLOCK_STATE, blockState)
                .param(BlockBreak.SOURCE_ENTITY, currentPacketSourcePlayer)
                .param(BlockBreak.HELD_ITEM_STACK, nmsTool.asBukkitCopy())
                .build()
        
        return getExp(block, blockState, ctx)
    }
    
    /**
     * Retrieves the amount of experience that would be dropped when breaking a block of [state] at [pos] with the given [ctx].
     */
    fun getExp(
        block: BukkitBlock,
        state: NovaBlockState,
        ctx: Context<BlockBreak>
    ): Int = runSafely("get exp", 0) {
        return behaviors.sumOf { it.getExp(block, state, ctx) }
    }
    
    override fun getCloneItemStack(
        nmsLevel: LevelReader,
        nmsPos: NmsBlockPos,
        nmsBlockState: NmsBlockState,
        includeData: Boolean
    ): NmsItemStack {
        if (nmsLevel !is ServerLevel) // shouldn't be possible
            return NmsItemStack.EMPTY
        
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = Context.intention(BlockInteract)
            .param(BlockInteract.BLOCK, block)
            .param(BlockInteract.BLOCK_STATE, blockState)
            .param(BlockInteract.INCLUDE_DATA, includeData)
            .param(BlockInteract.SOURCE_ENTITY, currentPacketSourcePlayer)
            .build()
        
        return runSafely("pick block creative", { item?.createItemStack() }) {
            behaviors.firstNotNullOfOrNull { it.pickBlockCreative(block, blockState, ctx) } ?: item?.createItemStack()
        }.unwrap()
    }
    
    private inline fun runSafely(name: String, run: () -> Unit) = runSafely(name, Unit, run)
    
    private inline fun <T> runSafely(name: String, fallback: T, run: () -> T): T {
        checkServerThread()
        try {
            return run()
        } catch (t: Throwable) {
            LOGGER.error("Failed to $name for ${key.asString()}", t)
        }
        return fallback
    }
    
    private inline fun <T> runSafely(name: String, lazyFallback: () -> T, run: () -> T): T {
        checkServerThread()
        try {
            return run()
        } catch (t: Throwable) {
            LOGGER.error("Failed to $name for ${key.asString()}", t)
        }
        return lazyFallback()
    }
    //</editor-fold>
    
    override fun toString(): String = key.asString()
    
    companion object {
        
        // hack to make properties available in createBlockStateDefinition 
        // (called from super constructor, where a field wouldn't be initialized yet)
        val STATE_PROPERTIES: ScopedValue<List<BlockStateProperty<*>>> = ScopedValue.newInstance()
        
    }
    
}
