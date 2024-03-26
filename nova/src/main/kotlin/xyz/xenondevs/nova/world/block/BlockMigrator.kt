package xyz.xenondevs.nova.world.block

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.util.world.ChunkSearchQuery
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import java.util.function.Predicate
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object BlockMigrator : Listener {
    
    private val CHUNK_SEARCH_ID_KEY = NamespacedKey(NOVA_PLUGIN, "chunk_search_id")
    private var chunkSearchId by PermanentStorage.storedValue("chunk_search_id") { Random.nextInt() }
    
    private val blockMigrators = ArrayList<Pair<ChunkSearchQuery, (BlockPos) -> Unit>>()
    private val queries: List<ChunkSearchQuery>
        get() = blockMigrators.map { it.first }
    
    @InitFun
    private fun init() {
        registerEvents()
        Bukkit.getWorlds().flatMap(World::getLoadedChunks).forEach(BlockMigrator::searchChunk)
        
        registerBlockMigrator(
            { it.block == Blocks.RED_MUSHROOM_BLOCK },
            { it.setBlockStateSilently(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()) }
        )
        
        registerBlockMigrator(
            { it.block == Blocks.BROWN_MUSHROOM_BLOCK },
            { it.setBlockStateSilently(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()) }
        )
        
        registerBlockMigrator(
            { it.block == Blocks.MUSHROOM_STEM },
            { it.setBlockStateSilently(Blocks.MUSHROOM_STEM.defaultBlockState()) }
        )
        
        registerBlockMigrator(
            { it.block == Blocks.NOTE_BLOCK },
            { pos ->
                pos.setBlockStateSilently(Blocks.NOTE_BLOCK.defaultBlockState())
                WorldDataManager.setBlockState(pos, DefaultBlocks.NOTE_BLOCK.defaultBlockState)
            }
        )
    }
    
    private fun registerBlockMigrator(check: (BlockState) -> Boolean, handler: (BlockPos) -> Unit) {
        blockMigrators += Predicate(check) to handler
    }
    
    @EventHandler
    private fun searchChunk(event: ChunkLoadEvent) {
        searchChunk(event.chunk)
    }
    
    private fun searchChunk(chunk: Chunk) {
        val pdc = chunk.persistentDataContainer
        
        // TODO
//        if (pdc.get(CHUNK_SEARCH_ID_KEY, PersistentDataType.INTEGER) == chunkSearchId)
//            return
        
        BlockStateSearcher.searchChunk(chunk.pos, queries)
            .withIndex()
            .forEach { (idx, result) ->
                if (result == null)
                    return@forEach
                
                val handler = blockMigrators[idx].second
                for (pos in result) {
                    if (WorldDataManager.getBlockState(pos) == null && CustomItemServiceManager.getBlockType(pos.block) == null)
                        handler(pos)
                }
            }
        
        pdc.set(CHUNK_SEARCH_ID_KEY, PersistentDataType.INTEGER, chunkSearchId)
    }
    
    @EventHandler
    private fun handlePhysics(event: BlockPhysicsEvent) {
        runBlockMigrations(event.block.pos)
    }
    
    private fun runBlockMigrations(pos: BlockPos) {
        if (WorldDataManager.getBlockState(pos) != null || CustomItemServiceManager.getBlockType(pos.block) != null)
            return
        
        for ((check, handler) in blockMigrators) {
            if (check.test(pos.nmsBlockState))
                handler(pos)
        }
    }
    
    fun updateChunkSearchId() {
        chunkSearchId = Random.nextInt(0, Int.MAX_VALUE)
    }
    
}