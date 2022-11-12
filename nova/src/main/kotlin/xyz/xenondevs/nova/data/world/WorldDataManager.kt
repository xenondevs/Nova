package xyz.xenondevs.nova.data.world

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.event.NovaChunkLoadedEvent
import xyz.xenondevs.nova.data.world.legacy.LegacyFileConverter
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.concurrent.Latch
import xyz.xenondevs.nova.util.pollFirstWhere
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write

internal object WorldDataManager : Initializable(), Listener {
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(AddonsInitializer, LegacyFileConverter, TileEntityManager, VanillaTileEntityManager, NetworkManager)
    
    private val worlds: MutableMap<UUID, WorldDataStorage> = Collections.synchronizedMap(HashMap()) // TODO: removing entries of unloaded worlds
    
    private val saveTasks = ConcurrentLinkedQueue<World>()
    private val chunkTasks: MutableMap<ChunkPos, Boolean> = Collections.synchronizedMap(HashMap())
    private val chunkLocks: MutableMap<ChunkPos, Latch> = Collections.synchronizedMap(HashMap())
    
    override fun init() {
        LOGGER.info("Initializing WorldDataManager")
        registerEvents()
        Bukkit.getWorlds().forEach(::queueWorldLoad)
        
        thread(name = "Nova WorldDataManager", isDaemon = true) { // TODO: Use Phaser instead of Thread.sleep
            while (NOVA.isEnabled) {
                
                while (saveTasks.isNotEmpty()) {
                    try {
                        val world = saveTasks.poll()
                        saveWorld(world)
                    } catch (t: Throwable) {
                        LOGGER.log(Level.SEVERE, "An exception occurred trying to save a world", t)
                    }
                }
                
                while (chunkTasks.isNotEmpty()) {
                    try {
                        // Take the first chunk task that isn't busy
                        val (pos, goal) = synchronized(chunkTasks) {
                            chunkTasks.entries.pollFirstWhere { chunkLocks[it.key]?.isClosed() != true }
                        } ?: break
                        
                        // Mark chunk pos as busy
                        val latch = Latch()
                        latch.close()
                        chunkLocks[pos] = latch
                        
                        // Perform task
                        if (goal) loadChunk(pos, latch)
                        else unloadChunk(pos, latch)
                    } catch (t: Throwable) {
                        LOGGER.log(Level.SEVERE, "An exception occurred trying to load or unload a chunk", t)
                    }
                }
                
                Thread.sleep(50)
            }
        }
    }
    
    override fun disable() {
        Bukkit.getWorlds().forEach(::saveWorld)
    }
    
    //<editor-fold desc="queueing tasks", defaultstate="collapsed">
    private fun queueChunkLoad(pos: ChunkPos) {
        chunkTasks[pos] = true
    }
    
    private fun queueChunkUnload(pos: ChunkPos) {
        chunkTasks[pos] = false
    }
    
    private fun queueWorldLoad(world: World) {
        synchronized(chunkTasks) {
            world.loadedChunks.forEach { chunkTasks[it.pos] = true }
        }
    }
    
    private fun queueWorldUnload(world: World) {
        synchronized(chunkTasks) {
            world.loadedChunks.forEach { chunkTasks[it.pos] = false }
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="loading / unloading chunks", defaultstate="collapsed">
    private fun loadChunk(pos: ChunkPos, latch: Latch) {
        if (pos.isLoaded()) {
            // loads region from file if not already loaded
            val chunk = getRegion(pos).getChunk(pos)
            
            // the rest needs to be done in the server thread
            runTask {
                try {
                    if (pos.isLoaded()) {
                        val blockStates = chunk.lock.write {
                            // is RegionChunk already loaded?
                            if (chunk.isLoaded)
                                return@runTask
                            
                            // copy blockstates map, only remove failed states from the event blockstates map
                            val blockStates = HashMap(chunk.blockStates).removeIf { (_, blockState) ->
                                runCatching { blockState.handleInitialized(false) }
                                    .onFailure { LOGGER.log(Level.SEVERE, "Failed to initialize $blockState", it) }
                                    .isFailure
                            }
                            
                            // mark RegionChunk as loaded
                            chunk.isLoaded = true
                            
                            return@write blockStates
                        }
                        
                        // call event
                        val event = NovaChunkLoadedEvent(pos, blockStates)
                        Bukkit.getPluginManager().callEvent(event)
                    }
                } catch (t: Throwable) {
                    LOGGER.log(Level.SEVERE, "Failed to load chunk $pos", t)
                }
                
                // task is completed
                latch.open()
            }
        }
        
        // task will not run
        latch.open()
    }
    
    private fun unloadChunk(pos: ChunkPos, latch: Latch) {
        // get chunk
        val chunk = worlds[pos.worldUUID]
            ?.getRegionOrNull(pos)
            ?.getChunk(pos)
            ?: return
        
        chunk.lock.write {
            // check that the RegionChunk is actually loaded
            if (!chunk.isLoaded)
                return
            // unload all block states
            chunk.blockStates.values.forEach { it.handleRemoved(false) }
            // mark RegionChunk as unloaded
            chunk.isLoaded = false
        }
        
        // task is completed
        latch.open()
    }
    
    private fun saveWorld(world: World) {
        worlds[world.uid]?.saveAll()
    }
    //</editor-fold>
    
    //<editor-fold desc="reading from / writing to chunks", defaultstate="collapsed">
    fun getBlockStates(pos: ChunkPos, takeUnloaded: Boolean = false): Map<BlockPos, BlockState> =
        readChunk(pos) { chunk -> HashMap(chunk.blockStates) }.apply { if (!takeUnloaded) removeIf { !it.value.isLoaded } }
    
    fun getBlockState(pos: BlockPos, takeUnloaded: Boolean = false): BlockState? =
        readChunk(pos.chunkPos) { chunk -> chunk.blockStates[pos]?.takeIf { takeUnloaded || it.isLoaded } }
    
    fun setBlockState(pos: BlockPos, state: BlockState) =
        writeChunk(pos.chunkPos) { it.blockStates[pos] = state }
    
    fun removeBlockState(pos: BlockPos) =
        writeChunk(pos.chunkPos) { it.blockStates -= pos }
    
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.getOrPut(world.uid) { WorldDataStorage(world) }
    
    private fun getRegion(pos: ChunkPos): RegionFile =
        getWorldStorage(pos.world!!).getRegion(pos)
    
    private inline fun <T> readChunk(pos: ChunkPos, read: (RegionChunk) -> T): T {
        val region = getRegion(pos)
        val chunk = region.getChunk(pos)
        chunk.lock.read {
            return read.invoke(chunk)
        }
    }
    
    private inline fun <T> writeChunk(pos: ChunkPos, write: (RegionChunk) -> T): T {
        val region = getRegion(pos)
        val chunk = region.getChunk(pos)
        chunk.lock.write {
            return write.invoke(chunk)
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="event handlers", defaultstate="collapsed">
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        queueWorldUnload(event.world)
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            queueChunkLoad(event.chunk.pos)
        }
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            queueChunkUnload(event.chunk.pos)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            saveTasks += event.world
        }
    }
    //</editor-fold>
    
}