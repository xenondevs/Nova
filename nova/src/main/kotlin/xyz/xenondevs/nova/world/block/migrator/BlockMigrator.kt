package xyz.xenondevs.nova.world.block.migrator

import kotlinx.coroutines.runBlocking
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.associateByNotNull
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.patch.impl.worldgen.chunksection.LevelChunkSectionWrapper
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.util.bukkitMaterial
import xyz.xenondevs.nova.util.instrument
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.model.AcaciaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.AzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BirchLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BlockUpdateMethod
import xyz.xenondevs.nova.world.block.state.model.BrownMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.CherryLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DarkOakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DefaultingBackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.FloweringAzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.JungleLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MangroveLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MushroomStemBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.NoteBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.OakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.SpruceLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import java.util.logging.Level
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [
        ResourceGeneration.PreWorld::class,
        WorldDataManager::class
    ]
)
internal object BlockMigrator : Listener {
    
    private val MIGRATION_ID_KEY = NamespacedKey(NOVA, "migration_id")
    private var migrationId by PermanentStorage.storedValue("migration_id") { Random.nextInt() }
    
    private val migrations = ArrayList<BlockMigration>()
    private val _migrationsByVanillaBlock = HashMap<Block, BlockMigration>()
    private val _migrationsByNovaBlock = HashMap<NovaBlock, BlockMigration>()
    private val queries = ArrayList<(BlockState) -> Boolean>()
    
    val migrationsByVanillaBlock: Map<Block, BlockMigration>
        get() = _migrationsByVanillaBlock
    val migrationsByNovaBlock: Map<NovaBlock, BlockMigration>
        get() = _migrationsByNovaBlock
    
    @InitFun
    private fun init() {
        addMigrations()
        registerEvents()
        migrateLoadedChunks()
    }
    
    private fun addMigrations() {
        migrations += BlockMigration(
            Blocks.RED_MUSHROOM_BLOCK, null,
            RedMushroomBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += BlockMigration(
            Blocks.BROWN_MUSHROOM_BLOCK, null,
            BrownMushroomBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += BlockMigration(
            Blocks.MUSHROOM_STEM, null,
            MushroomStemBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += BlockMigration(
            Blocks.NOTE_BLOCK, DefaultBlocks.NOTE_BLOCK,
            NoteBackingStateConfig.defaultStateConfig.vanillaBlockState,
            { vanilla ->
                DefaultBlocks.NOTE_BLOCK.defaultBlockState
                    .with(DefaultBlockStateProperties.NOTE_BLOCK_INSTRUMENT, vanilla.getValue(NoteBlock.INSTRUMENT).instrument)
                    .with(DefaultBlockStateProperties.NOTE_BLOCK_NOTE, vanilla.getValue(NoteBlock.NOTE))
                    .with(DefaultBlockStateProperties.POWERED, vanilla.getValue(NoteBlock.POWERED))
            }
        )
        
        migrations += leavesMigration(Blocks.OAK_LEAVES, OakLeavesBackingStateConfig, DefaultBlocks.OAK_LEAVES)
        migrations += leavesMigration(Blocks.SPRUCE_LEAVES, SpruceLeavesBackingStateConfig, DefaultBlocks.SPRUCE_LEAVES)
        migrations += leavesMigration(Blocks.BIRCH_LEAVES, BirchLeavesBackingStateConfig, DefaultBlocks.BIRCH_LEAVES)
        migrations += leavesMigration(Blocks.JUNGLE_LEAVES, JungleLeavesBackingStateConfig, DefaultBlocks.JUNGLE_LEAVES)
        migrations += leavesMigration(Blocks.ACACIA_LEAVES, AcaciaLeavesBackingStateConfig, DefaultBlocks.ACACIA_LEAVES)
        migrations += leavesMigration(Blocks.DARK_OAK_LEAVES, DarkOakLeavesBackingStateConfig, DefaultBlocks.DARK_OAK_LEAVES)
        migrations += leavesMigration(Blocks.MANGROVE_LEAVES, MangroveLeavesBackingStateConfig, DefaultBlocks.MANGROVE_LEAVES)
        migrations += leavesMigration(Blocks.CHERRY_LEAVES, CherryLeavesBackingStateConfig, DefaultBlocks.CHERRY_LEAVES)
        migrations += leavesMigration(Blocks.AZALEA_LEAVES, AzaleaLeavesBackingStateConfig, DefaultBlocks.AZALEA_LEAVES)
        migrations += leavesMigration(Blocks.FLOWERING_AZALEA_LEAVES, FloweringAzaleaLeavesBackingStateConfig, DefaultBlocks.FLOWERING_AZALEA_LEAVES)
        
        queries += migrations.map { migration -> { state -> state.block == migration.vanillaBlock } }
        queries += { state -> VanillaTileEntity.Type.of(state.block.bukkitMaterial) != null }
        
        _migrationsByVanillaBlock += migrations.associateBy { it.vanillaBlock }
        _migrationsByNovaBlock += migrations.associateByNotNull { it.novaBlock }
    }
    
    private fun leavesMigration(block: Block, cfg: DefaultingBackingStateConfigType<*>, novaBlock: NovaBlock): BlockMigration {
        return BlockMigration(
            block, novaBlock,
            cfg.defaultStateConfig.vanillaBlockState,
            { vanilla ->
                novaBlock.defaultBlockState
                    .with(DefaultBlockStateProperties.LEAVES_DISTANCE, vanilla.getValue(LeavesBlock.DISTANCE))
                    .with(DefaultBlockStateProperties.LEAVES_PERSISTENT, vanilla.getValue(LeavesBlock.PERSISTENT))
                    .with(DefaultBlockStateProperties.WATERLOGGED, vanilla.getValue(LeavesBlock.WATERLOGGED))
            },
            { nova ->
                block.defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, nova.getOrThrow(DefaultBlockStateProperties.LEAVES_DISTANCE))
                    .setValue(LeavesBlock.PERSISTENT, nova.getOrThrow(DefaultBlockStateProperties.LEAVES_PERSISTENT))
                    .setValue(LeavesBlock.WATERLOGGED, nova.getOrThrow(DefaultBlockStateProperties.WATERLOGGED))
            }
        )
    }
    
    private fun migrateLoadedChunks() {
        Bukkit.getWorlds()
            .flatMap(World::getLoadedChunks)
            .forEach(::migrateChunk)
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        migrateChunk(event.chunk)
    }
    
    private fun migrateChunk(chunk: Chunk) {
        for (section in chunk.levelChunk.sections) {
            (section as LevelChunkSectionWrapper).isMigrationActive = true
        }
        
        val pdc = chunk.persistentDataContainer
        if (pdc.get(MIGRATION_ID_KEY, PersistentDataType.INTEGER) == migrationId)
            return
        
        // migrate Nova backing states
        val regionChunk = runBlocking { WorldDataManager.getOrLoadChunk(chunk.pos) } // should already be loaded in most cases
        regionChunk.forEachNonEmpty { pos, blockState ->
            blockState.modelProvider.replace(pos, BlockUpdateMethod.SILENT)
        }
        
        // migrate vanilla block states that are used by Nova 
        val levelChunk = chunk.levelChunk
        BlockStateSearcher.searchChunk(chunk.pos, queries)
            .withIndex()
            .forEach { (_, result) ->
                if (result == null)
                    return@forEach
                
                for ((pos, blockState) in result) {
                    if (WorldDataManager.getBlockState(pos) == null &&
                        CustomItemServiceManager.getBlockType(pos.block) == null
                    ) {
                        pos.setBlockStateSilently(blockState) // LevelChunkSectionWrapper will then call handleBlockStatePlaced 
                        val blockEntity = levelChunk.getBlockEntity(pos.nmsPos)
                        if (blockEntity != null && WorldDataManager.getVanillaTileEntity(pos) == null)
                            handleBlockEntityPlaced(pos, blockEntity)
                    }
                }
            }
        
        pdc.set(MIGRATION_ID_KEY, PersistentDataType.INTEGER, migrationId)
    }
    
    @JvmStatic
    fun migrateBlockState(pos: BlockPos, blockState: BlockState): BlockState {
        try {
            var migratedState = migrationsByVanillaBlock[blockState.block]?.vanillaBlockState
            if (migratedState != null) {
                // pass waterlogged property through
                if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    migratedState = migratedState.setValue(
                        BlockStateProperties.WATERLOGGED,
                        blockState.getValue(BlockStateProperties.WATERLOGGED)
                    )
                }
                return migratedState
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to migrate block state $blockState at $pos", e)
        }
        
        return blockState
    }
    
    /**
     * Handles a block state change at [pos] from [previousState] to [newState] that was not caused by Nova.
     */
    @JvmStatic
    fun handleBlockStatePlaced(pos: BlockPos, previousState: BlockState, newState: BlockState) {
        // Remove vte or notify of block state change
        val expectedVteType = VanillaTileEntity.Type.of(newState.block.bukkitMaterial)
        var vte = WorldDataManager.getVanillaTileEntity(pos)
        if (vte != null) {
            if (vte.type != expectedVteType) {
                WorldDataManager.setVanillaTileEntity(pos, null)
                vte.handleBreak()
                vte = null
            } else {
                vte.handleBlockStateChange(newState)
            }
        }
        // Not all vanilla tile entities are actually block entities.
        // For those, handleBlockEntityPlaced will never be fired, so we'll need to create the vte here.
        if (expectedVteType != null && vte == null && !expectedVteType.hasBlockEntity) {
            vte = expectedVteType.create(pos)
            WorldDataManager.setVanillaTileEntity(pos, vte)
            vte.handlePlace()
        }
        
        // Remove any existing nova block state / nova tile entity
        val previousNovaState = WorldDataManager.setBlockState(pos, null)
        val previousTileEntity = WorldDataManager.setTileEntity(pos, null)
        if ((previousNovaState != null && previousNovaState.block !in migrationsByNovaBlock) || previousTileEntity != null) {
            val ctx = Context.intention(DefaultContextIntentions.BlockBreak)
                .param(DefaultContextParamTypes.BLOCK_POS, pos)
                .param(DefaultContextParamTypes.BLOCK_STATE_NOVA, previousNovaState)
                .param(DefaultContextParamTypes.TILE_ENTITY_NOVA, previousTileEntity)
                .build()
            if (previousNovaState != null && previousNovaState.block !in migrationsByNovaBlock) {
                // call behavior break handlers directly to bypass any tile-entity or model provider related logic
                previousNovaState.block.behaviors.forEach { it.handleBreak(pos, previousNovaState, ctx) }
                // for entity-backed models, the display entity needs to be despawned
                if (previousNovaState.modelProvider.provider == DisplayEntityBlockModelProvider) {
                    DisplayEntityBlockModelProvider.unload(pos)
                }
            }
            previousTileEntity?.handleBreak(ctx)
        }
        
        // Migrations for block types that are also used as backing states
        val migration = migrationsByVanillaBlock[newState.block]
        val novaState = migration?.vanillaToNova?.invoke(newState)
        if (novaState != null) {
            WorldDataManager.setBlockState(pos, novaState)
        }
    }
    
    @JvmStatic
    fun handleBlockEntityPlaced(pos: BlockPos, blockEntity: BlockEntity?) {
        // We should generally be able to assume that block state changes happen before block entity removal / addition,
        // which means that there should never be a vanilla tile entity registered when this method is called.
        val previousVte = WorldDataManager.setVanillaTileEntity(pos, null)
        if (previousVte != null) {
            LOGGER.log(Level.SEVERE, "Vanilla tile entity $previousVte registered at $pos when handling block entity placed with $blockEntity", Exception())
            previousVte.handleBreak()
        }
        
        if (blockEntity != null) {
            val vteType = VanillaTileEntity.Type.of(blockEntity.blockState.bukkitMaterial)
                ?: return
            val vte = vteType.create(pos)
            WorldDataManager.setVanillaTileEntity(pos, vte)
            vte.handlePlace()
        }
    }
    
    fun updateMigrationId() {
        migrationId = Random.nextInt(0, Int.MAX_VALUE)
    }
    
}