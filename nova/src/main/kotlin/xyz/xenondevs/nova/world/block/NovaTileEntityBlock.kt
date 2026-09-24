package xyz.xenondevs.nova.world.block

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder
import kotlinx.coroutines.Job
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemType
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.ConfigProvider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.context.intention.ImplicitIntentions
import xyz.xenondevs.nova.registry.FlammableSettings
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.builder.layout.block.BlockSelectorScope
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.toBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import xyz.xenondevs.nova.LOGGER as NOVA_LOGGER

typealias TileEntityConstructor = ((Block, NovaBlockState, Compound) -> TileEntity)

private val COROUTINE_SUPERVISOR: VarHandle = MethodHandles.lookup()
    .findVarHandle(NewChunkHolder::class.java, $$"nova$coroutineSupervisor", Any::class.java)

internal val NewChunkHolder.coroutineSupervisor: Job?
    get() = COROUTINE_SUPERVISOR.get(this) as Job?

internal class NovaTileEntityBlock(
    entry: RegistryEntry.Paper<BlockType>,
    name: Provider<Component>,
    style: Provider<Style>,
    behaviors: Provider<List<BlockBehavior>>,
    stateProperties: List<BlockStateProperty<*>>,
    item: Provider<RegistryEntry.Paper<ItemType>?>,
    config: Provider<ConfigProvider>,
    properties: Provider<Properties>,
    flammable: Provider<FlammableSettings>,
    selectFluidFlowMode: Provider<BlockSelectorScope.() -> FluidFlowMode>,
    hitParticles: Provider<ItemType?>,
    breakParticles: Provider<BlockType?>,
    showBreakAnimation: Provider<Boolean>,
    val tileEntityConstructor: TileEntityConstructor,
    val tickrate: Provider<Int>
) : NovaBlock(
    entry,
    name,
    style,
    behaviors,
    stateProperties,
    item,
    config,
    properties,
    flammable,
    selectFluidFlowMode,
    hitParticles,
    breakParticles,
    showBreakAnimation
), EntityBlock {
    
    lateinit var blockEntityType: BlockEntityType<NovaTileEntityProxy>
    
    fun nmsHandlePlace(
        nmsBlockState: BlockState,
        nmsLevel: Level,
        nmsPos: BlockPos,
        nmsOldBlockState: BlockState,
        tileEntityProxy: NovaTileEntityProxy?
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val ctx = ImplicitIntentions.BLOCK_PLACE.getOrNull()
            ?: Context.intention(BlockPlace)
                .param(BlockPlace.BLOCK, block)
                .param(BlockPlace.BLOCK_STATE, blockState)
                .param(BlockPlace.PREVIOUS_BLOCK_STATE, nmsOldBlockState.bukkitBlockData)
                .build()
        
        tileEntityProxy?.tileEntity?.handlePlace(ctx)
        handlePlace(block, blockState, ctx)
    }
    
    fun nmsHandleBreak(
        nmsBlockState: BlockState,
        nmsLevel: Level,
        nmsPos: BlockPos,
        nmsNewBlockState: BlockState, // TODO: expose this or not?
        tileEntityProxy: NovaTileEntityProxy?
    ) {
        val block = nmsPos.toBlock(nmsLevel.world)
        val blockState = NovaBlockStateImpl(nmsBlockState)
        val tileEntity = tileEntityProxy?.tileEntity
        
        // TODO: also look for block place implicit intention as that can cause replacement -> break
        val ctx = (ImplicitIntentions.BLOCK_BREAK.getOrNull()?.toBuilder() ?: Context.intention(BlockBreak))
            .param(BlockBreak.BLOCK, block)
            .param(BlockBreak.BLOCK_STATE, blockState)
            .param(BlockBreak.TILE_ENTITY_NOVA, tileEntity)
            .build()
        
        handleBreak(block, blockState, ctx)
        tileEntity?.handleBreak(ctx)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(level: Level, blockState: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T> =
        NovaTileEntityTicker as BlockEntityTicker<T>
    
    override fun newBlockEntity(worldPosition: BlockPos, blockState: BlockState) =
        blockEntityType.create(worldPosition, blockState)
    
}

private object NovaTileEntityTicker : BlockEntityTicker<NovaTileEntityProxy> {
    
    override fun tick(level: Level, pos: BlockPos, state: BlockState, entity: NovaTileEntityProxy) {
        entity.tick(level, pos, state, entity)
    }
    
}

internal class NovaTileEntityProxy(
    private val block: NovaTileEntityBlock,
    worldPosition: BlockPos,
    blockState: BlockState
) : BlockEntity(block.blockEntityType, worldPosition, blockState), BlockEntityTicker<NovaTileEntityProxy> {
    
    private val tickOffset = Math.floorMod(worldPosition.hashCode(), 20)
    
    val data = Compound()
    var tileEntity: TileEntity? = null
        private set
    
    init {
        val ctx = ImplicitIntentions.BLOCK_PLACE.getOrNull()
        if (ctx != null) {
            val persistent = ctx[BlockPlace.TILE_ENTITY_DATA_NOVA]
                data["persistent"] = persistent
            val owner = ctx[BlockPlace.RESPONSIBLE_PLAYER]
            if (owner != null)
                data["ownerUuid"] = owner.uniqueId
        }
    }
    
    override fun setLevel(level: Level) {
        super.setLevel(level)
        if (tileEntity != null)
            return
        try {
            tileEntity = block.tileEntityConstructor(
                worldPosition.toBlock(level.world),
                NovaBlockStateImpl(blockState),
                data
            )
        } catch (t: Throwable) {
            xyz.xenondevs.nova.LOGGER.error("Failed to initialize tile entity for $blockState at $worldPosition with data $data", t)
        }
    }
    
    override fun setRemoved() {
        super.setRemoved()
        
        val tileEntity = tileEntity
            ?: return
        
        if (tileEntity.isTicking) {
            tileEntity.isTicking = false
            tileEntity.coroutineSupervisor?.cancel()
            tileEntity.coroutineSupervisor = null
            tileEntity.handleDisableTicking()
        }
        if (tileEntity.isEnabled) {
            tileEntity.isEnabled = false
            tileEntity.handleDisable()
        }
    }
    
    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        runSafely("load tile entity data") {
            val additional: Compound? = persistentDataContainer
                .get(TileEntity.TILE_ENTITY_DATA_KEY, PersistentDataType.BYTE_ARRAY)
                ?.let(Cbf::read)
            if (additional != null)
                data.putAll(additional)
        }
    }
    
    override fun saveAdditional(output: ValueOutput) {
        runSafely("save tile entity data") {
            tileEntity?.saveData()
            persistentDataContainer.set(TileEntity.TILE_ENTITY_DATA_KEY, PersistentDataType.BYTE_ARRAY, Cbf.write(data))
        }
        super.saveAdditional(output)
    }
    
    override fun sanitizeSentNbt(tag: CompoundTag): CompoundTag {
        tag.remove("nova")
        return super.sanitizeSentNbt(tag)
    }
    
    private inline fun runSafely(name: String, run: () -> Unit) = runSafely(name, {}, run)
    
    private inline fun <T> runSafely(name: String, fallback: () -> T, run: () -> T): T {
        try {
            return run()
        } catch (e: Exception) {
            NOVA_LOGGER.error("Failed to $name for ${block.key.asString()} at ${worldPosition.x}, ${worldPosition.y}, ${worldPosition.z} in ${level?.dimension()?.identifier()}", e)
        }
        return fallback()
    }
    
    override fun tick(level: Level, pos: BlockPos, state: BlockState, entity: NovaTileEntityProxy) {
        val tickrate = block.tickrate.get()
        if (tickrate == 0)
            return
        
        if ((level.gameTime * tickrate + tickOffset) % 20L < tickrate)
            tileEntity?.handleTick()
    }
    
}