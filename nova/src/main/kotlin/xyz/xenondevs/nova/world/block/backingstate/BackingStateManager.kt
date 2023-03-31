package xyz.xenondevs.nova.world.block.backingstate

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
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.util.world.ChunkSearchQuery
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.backingstate.impl.BrownMushroomBlockBackingState
import xyz.xenondevs.nova.world.block.backingstate.impl.MushroomStemBackingState
import xyz.xenondevs.nova.world.block.backingstate.impl.NoteBlockBackingState
import xyz.xenondevs.nova.world.block.backingstate.impl.RedMushroomBlockBackingState
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.random.Random

private val CHUNK_SEARCH_ID_KEY = NamespacedKey(NOVA, "chunkSearchId")

@InternalInit(
    stage = InitializationStage.POST_WORLD,
    dependsOn = [WorldDataManager::class, Patcher::class]
)
internal object BackingStateManager : Listener {
    
    private val chunkSearchQueue = ConcurrentLinkedQueue<ChunkPos>()
    
    private var chunkSearchId = PermanentStorage.retrieve("chunkSearchId") { -1 }
        set(value) {
            field = value
            PermanentStorage.store("chunkSearchId", value)
        }
    
    private val backingStates: List<BackingState> = listOf(
        NoteBlockBackingState,
        RedMushroomBlockBackingState, BrownMushroomBlockBackingState, MushroomStemBackingState
    )
    
    private val queries: List<ChunkSearchQuery> =
        backingStates.map { it.blockStatePredicate }
    
    @InitFun
    private fun init() {
        if (!DEFAULT_CONFIG.getBoolean("resource_pack.generation.use_solid_blocks"))
            return
        
        LOGGER.info("Using block behaviors: ${backingStates.joinToString { it::class.simpleName!! }}")
        
        if (chunkSearchId == -1)
            updateChunkSearchId()
        
        registerEvents()
        backingStates.forEach(BackingState::init)
        
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
                        
                        BlockStateSearcher.searchChunk(chunkPos, queries)
                            .withIndex()
                            .forEach { (idx, result) ->
                                if (result == null)
                                    return@forEach
                                
                                backingStates[idx].handleQueryResult(result)
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
        val block = event.block
        if (CustomItemServiceManager.getBlockType(block) != null)
            return
        
        val blockState = block.nmsState
        val behavior = backingStates.firstOrNull { blockState.block == it.defaultState.block } ?: return
        val pos = block.pos
        
        val task = {
            val correctState = behavior.getCorrectBlockState(pos)
            if (correctState != null) pos.setBlockStateSilently(correctState)
        }
        
        if (behavior.runUpdateLater)
            runTask(task)
        else task()
    }
    
}