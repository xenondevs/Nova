package xyz.xenondevs.nova.world.block.migrator

import kotlinx.coroutines.runBlocking
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.TripWireBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
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
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.bukkitMaterial
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.model.BlockUpdateMethod
import xyz.xenondevs.nova.world.block.state.model.BrownMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.MushroomStemBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.isMigrationActive
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [
        ResourceGeneration.PreWorld::class,
        WorldDataManager::class
    ]
)
internal object BlockMigrator : Listener {
    
    private val MIGRATION_ID_KEY = NamespacedKey("nova", "migration_id")
    private var migrationId by PermanentStorage.storedValue("migration_id") { Random.nextInt() }
    
    private val migrations = ArrayList<BlockMigration>()
    private val migrationsByVanillaBlock = HashMap<Block, BlockMigration>()
    private val migrationsByNovaBlock = HashMap<NovaBlock, BlockMigration>()
    private val queries = ArrayList<(BlockState) -> Boolean>()
    
    @JvmField
    val migrationSuppression: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
    
    @InitFun
    private fun init() {
        addMigrations()
        registerEvents()
        migrateLoadedChunks()
    }
    
    private fun addMigrations() {
        migrations += SimpleBlockMigration(
            Blocks.RED_MUSHROOM_BLOCK,
            RedMushroomBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += SimpleBlockMigration(
            Blocks.BROWN_MUSHROOM_BLOCK,
            BrownMushroomBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += SimpleBlockMigration(
            Blocks.MUSHROOM_STEM,
            MushroomStemBackingStateConfig.defaultStateConfig.vanillaBlockState
        )
        
        migrations += ComplexBlockMigration(
            Blocks.NOTE_BLOCK, DefaultBlocks.NOTE_BLOCK
        ) { vanilla ->
            DefaultBlocks.NOTE_BLOCK.defaultBlockState
                .with(DefaultBlockStateProperties.NOTE_BLOCK_INSTRUMENT, vanilla.getValue(NoteBlock.INSTRUMENT))
                .with(DefaultBlockStateProperties.NOTE_BLOCK_NOTE, vanilla.getValue(NoteBlock.NOTE))
                .with(DefaultBlockStateProperties.POWERED, vanilla.getValue(NoteBlock.POWERED))
        }
        
        migrations += ComplexBlockMigration(
            Blocks.TRIPWIRE, DefaultBlocks.TRIPWIRE
        ) { vanilla ->
            DefaultBlocks.TRIPWIRE.defaultBlockState
                .with(DefaultBlockStateProperties.TRIPWIRE_NORTH, vanilla.getValue(TripWireBlock.NORTH))
                .with(DefaultBlockStateProperties.TRIPWIRE_EAST, vanilla.getValue(TripWireBlock.EAST))
                .with(DefaultBlockStateProperties.TRIPWIRE_SOUTH, vanilla.getValue(TripWireBlock.SOUTH))
                .with(DefaultBlockStateProperties.TRIPWIRE_WEST, vanilla.getValue(TripWireBlock.WEST))
                .with(DefaultBlockStateProperties.TRIPWIRE_ATTACHED, vanilla.getValue(TripWireBlock.ATTACHED))
                .with(DefaultBlockStateProperties.TRIPWIRE_DISARMED, vanilla.getValue(TripWireBlock.DISARMED))
                .with(DefaultBlockStateProperties.POWERED, vanilla.getValue(TripWireBlock.POWERED))
        }
        
        migrations += leavesMigration(Blocks.OAK_LEAVES, DefaultBlocks.OAK_LEAVES)
        migrations += leavesMigration(Blocks.SPRUCE_LEAVES, DefaultBlocks.SPRUCE_LEAVES)
        migrations += leavesMigration(Blocks.BIRCH_LEAVES, DefaultBlocks.BIRCH_LEAVES)
        migrations += leavesMigration(Blocks.JUNGLE_LEAVES, DefaultBlocks.JUNGLE_LEAVES)
        migrations += leavesMigration(Blocks.ACACIA_LEAVES, DefaultBlocks.ACACIA_LEAVES)
        migrations += leavesMigration(Blocks.DARK_OAK_LEAVES, DefaultBlocks.DARK_OAK_LEAVES)
        migrations += leavesMigration(Blocks.MANGROVE_LEAVES, DefaultBlocks.MANGROVE_LEAVES)
        migrations += leavesMigration(Blocks.CHERRY_LEAVES, DefaultBlocks.CHERRY_LEAVES)
        migrations += leavesMigration(Blocks.AZALEA_LEAVES, DefaultBlocks.AZALEA_LEAVES)
        migrations += leavesMigration(Blocks.FLOWERING_AZALEA_LEAVES, DefaultBlocks.FLOWERING_AZALEA_LEAVES)
        migrations += leavesMigration(Blocks.PALE_OAK_LEAVES, DefaultBlocks.PALE_OAK_LEAVES)
        
        queries += migrations.map { migration -> { state -> state.block == migration.vanillaBlock } }
        queries += { state -> VanillaTileEntity.Type.of(state.block.bukkitMaterial) != null }
        
        migrationsByVanillaBlock += migrations.associateBy { it.vanillaBlock }
        migrationsByNovaBlock += migrations.filterIsInstance<ComplexBlockMigration>().associateByNotNull { it.novaBlock }
    }
    
    private fun leavesMigration(block: Block, novaBlock: NovaBlock): BlockMigration {
        return ComplexBlockMigration(
            block, novaBlock
        ) { vanilla ->
            novaBlock.defaultBlockState
                .with(DefaultBlockStateProperties.LEAVES_DISTANCE, vanilla.getValue(LeavesBlock.DISTANCE))
                .with(DefaultBlockStateProperties.LEAVES_PERSISTENT, vanilla.getValue(LeavesBlock.PERSISTENT))
                .with(DefaultBlockStateProperties.WATERLOGGED, vanilla.getValue(LeavesBlock.WATERLOGGED))
        }
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
            section.isMigrationActive = true
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
        BlockStateSearcher.searchChunk(chunk.pos, queries).forEach { result ->
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
        // disable migrations for all block states used by base packs
        if (blockState in ResourceLookups.OCCUPIED_BLOCK_STATES)
            return blockState
        
        try {
            val migration = migrationsByVanillaBlock[blockState.block]
                ?: return blockState
            
            return when (migration) {
                is SimpleBlockMigration -> migration.vanillaBlockState
                is ComplexBlockMigration -> migration.novaToVanilla(migration.vanillaToNova(blockState))
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to migrate block state $blockState at $pos", e)
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
            val ctx = Context.intention(BlockBreak)
                .param(BlockBreak.BLOCK_POS, pos)
                .param(BlockBreak.BLOCK_STATE_NOVA, previousNovaState)
                .param(BlockBreak.TILE_ENTITY_NOVA, previousTileEntity)
                .build()
            if (previousNovaState != null && previousNovaState.block !in migrationsByNovaBlock) {
                // call behavior break handlers directly to bypass any tile-entity or model provider related logic
                previousNovaState.block.behaviors.forEach { it.handleBreak(pos, previousNovaState, ctx) }
                // for entity-backed models, the display entity needs to be despawned
                (previousNovaState.modelProvider as? DisplayEntityBlockModelProvider)?.unload(pos)
            }
            previousTileEntity?.handleBreak(ctx)
        }
        
        // Migrations for block types that are also used as backing states
        if (newState !in ResourceLookups.OCCUPIED_BLOCK_STATES) {
            val migration = migrationsByVanillaBlock[newState.block]
            if (migration is ComplexBlockMigration) {
                val novaState = migration.vanillaToNova(newState)
                WorldDataManager.setBlockState(pos, novaState)
            }
        }
    }
    
    @JvmStatic
    fun handleBlockEntityPlaced(pos: BlockPos, blockEntity: BlockEntity?) {
        // We should generally be able to assume that block state changes happen before block entity removal / addition,
        // which means that there should never be a vanilla tile entity registered when this method is called.
        val previousVte = WorldDataManager.setVanillaTileEntity(pos, null)
        if (previousVte != null) {
            LOGGER.error("Vanilla tile entity $previousVte registered at $pos when handling block entity placed with $blockEntity", Exception())
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