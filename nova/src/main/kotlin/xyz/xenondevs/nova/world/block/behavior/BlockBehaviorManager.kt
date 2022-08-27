package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.flatMap
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.util.world.ChunkSearchQuery
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.behavior.impl.BrownMushroomBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.MushroomStemBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.RedMushroomBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.impl.noteblock.NoteBlockBehavior
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.random.Random

private val CHUNK_SEARCH_ID_KEY = NamespacedKey(NOVA, "chunkSearchId")

internal object BlockBehaviorManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(WorldDataManager, Patcher)
    
    private val chunkSearchQueue = ConcurrentLinkedQueue<ChunkPos>()
    
    private var chunkSearchId = PermanentStorage.retrieve("chunkSearchId") { -1 }
        set(value) {
            field = value
            PermanentStorage.store("chunkSearchId", value)
        }
    
    private val behaviors: List<BlockBehavior> = listOf(
        NoteBlockBehavior,
        RedMushroomBlockBehavior,
        BrownMushroomBlockBehavior,
        MushroomStemBlockBehavior
    )
    
    private val behaviorQueries: List<ChunkSearchQuery> =
        behaviors.map { it.blockStatePredicate }
    
    override fun init() {
        if (!DEFAULT_CONFIG.getBoolean("resource_pack.use_solid_blocks"))
            return
        
        LOGGER.info("Using block behaviors: ${behaviors.joinToString { it::class.simpleName!! }}")
        
        if (chunkSearchId == -1)
            updateChunkSearchId()
        
        registerEvents()
        behaviors.forEach(BlockBehavior::init)
        
        startChunkSearcher()
        Bukkit.getWorlds().flatMap(World::getLoadedChunks).forEach(::handleChunkLoad)
    }
    
    private fun startChunkSearcher() {
        thread(isDaemon = true, name = "Nova Chunk Searcher") {
            while (NOVA.isEnabled) {
                try {
                    while (chunkSearchQueue.isNotEmpty()) {
                        val chunkPos = chunkSearchQueue.poll()
                        
                        if (!chunkPos.isLoaded())
                            continue
                        
                        BlockStateSearcher.searchChunk(chunkPos, behaviorQueries)
                            .withIndex()
                            .forEach { (idx, result) ->
                                if (result == null)
                                    return@forEach
                                
                                behaviors[idx].handleQueryResult(result)
                            }
                        
                        runTask {
                            chunkPos.chunk!!.persistentDataContainer.set(CHUNK_SEARCH_ID_KEY, PersistentDataType.INTEGER, chunkSearchId)
                        }
                    }
                    
                    Thread.sleep(50)
                } catch (e: Exception) {
                    LOGGER.log(Level.SEVERE, "An error occurred while doing a chunk search", e)
                }
            }
        }
    }
    
    fun updateChunkSearchId() {
        chunkSearchId = Random.nextInt(0, Int.MAX_VALUE)
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    private fun handleChunkLoad(chunk: Chunk) {
        if (chunk.persistentDataContainer.get(CHUNK_SEARCH_ID_KEY, PersistentDataType.INTEGER) != chunkSearchId) {
            chunkSearchQueue += chunk.pos
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val blockState = event.block.nmsState
        val behavior = behaviors.firstOrNull { blockState.block == it.defaultState.block } ?: return
        val pos = event.block.pos
        
        val task = {
            val correctState = behavior.getCorrectBlockState(pos)
            if (correctState != null) pos.setBlockStateSilently(correctState)
        }
        
        if (behavior.runUpdateLater)
            runTask(task)
        else task()
    }
    
}