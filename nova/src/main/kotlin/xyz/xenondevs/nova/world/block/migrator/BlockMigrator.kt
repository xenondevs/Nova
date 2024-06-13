package xyz.xenondevs.nova.world.block.migrator

import kotlinx.coroutines.runBlocking
import net.minecraft.world.level.block.Blocks
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.util.world.ChunkSearchQuery
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.state.model.BlockUpdateMethod
import xyz.xenondevs.nova.world.format.WorldDataManager
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
    
    private val MIGRATION_ID_KEY = NamespacedKey(NOVA_PLUGIN, "migration_id")
    private var migrationId by PermanentStorage.storedValue("migration_id") { Random.nextInt() }
    
    private val migrations = ArrayList<BlockMigration>()
    private val queries = ArrayList<ChunkSearchQuery>()
    
    @InitFun
    private fun init() {
        addMigrations()
        registerEvents()
        migrateLoadedChunks()
    }
    
    private fun addMigrations() {
        migrations += BlockMigration(
            { it.block == Blocks.RED_MUSHROOM_BLOCK },
            { it.setBlockStateSilently(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()) }
        )
        
        migrations += BlockMigration(
            { it.block == Blocks.BROWN_MUSHROOM_BLOCK },
            { it.setBlockStateSilently(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()) }
        )
        
        migrations += BlockMigration(
            { it.block == Blocks.MUSHROOM_STEM },
            { it.setBlockStateSilently(Blocks.MUSHROOM_STEM.defaultBlockState()) }
        )
        
        migrations += BlockMigration(
            { it.block == Blocks.NOTE_BLOCK },
            { pos ->
                pos.setBlockStateSilently(Blocks.NOTE_BLOCK.defaultBlockState())
                WorldDataManager.setBlockState(pos, DefaultBlocks.NOTE_BLOCK.defaultBlockState)
            }
        )
        
        queries += migrations.map { it.query }
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
        val pdc = chunk.persistentDataContainer
        
        if (pdc.get(MIGRATION_ID_KEY, PersistentDataType.INTEGER) == migrationId)
            return
        
        // migrate Nova backing states
        val regionChunk = runBlocking { WorldDataManager.getOrLoadChunk(chunk.pos) } // should already be loaded in most cases
        regionChunk.forEachNonEmpty { pos, blockState -> 
            blockState.modelProvider.replace(pos, BlockUpdateMethod.SILENT)
        }
        
        // migrate vanilla block states that are used by Nova 
        BlockStateSearcher.searchChunk(chunk.pos, queries)
            .withIndex()
            .forEach { (idx, result) ->
                if (result == null)
                    return@forEach
                
                val migration = migrations[idx]
                for (pos in result) {
                    if (WorldDataManager.getBlockState(pos) == null &&
                        CustomItemServiceManager.getBlockType(pos.block) == null
                    ) {
                        migration.migrate(pos)
                    }
                }
            }
        
        pdc.set(MIGRATION_ID_KEY, PersistentDataType.INTEGER, migrationId)
    }
    
    fun updateMigrationId() {
        migrationId = Random.nextInt(0, Int.MAX_VALUE)
    }
    
}