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
    
    private val worlds: MutableMap<World, WorldDataStorage> = Collections.synchronizedMap(HashMap())
    private val tasks = ConcurrentLinkedQueue<Task>() // TODO: Map RegionFile -> Queue
    
    override fun init() {
        LOGGER.info("Initializing WorldDataManager")
        registerEvents()
        tasks += Bukkit.getWorlds().flatMap { world -> world.loadedChunks.map { ChunkLoadTask(it.pos) } }
        
        thread(name = "Nova WorldDataManager", isDaemon = true) { // TODO: Use Phaser instead of Thread.sleep
            while (NOVA.isEnabled) {
                while (tasks.isNotEmpty()) {
                    try {
                        when (val task = tasks.poll()) {
                            is ChunkLoadTask -> loadChunk(task.pos)
                            is ChunkUnloadTask -> unloadChunk(task.pos)
                            is SaveWorldTask -> saveWorld(task.world)
                            is WorldUnloadTask -> unloadWorld(task.world)
                        }
                    } catch (t: Throwable) {
                        LOGGER.log(Level.SEVERE, "An exception occurred in a WorldDataManager task", t)
                    }
                }
                Thread.sleep(50)
            }
        }
    }
    
    override fun disable() {
        Bukkit.getWorlds().forEach(::saveWorld)
    }
    
    private fun loadChunk(pos: ChunkPos) {
        if (pos.isLoaded()) {
            runTask {
                if (pos.isLoaded()) {
                    val blockStates = readChunk(pos) { HashMap(it.blockStates) }
                        .removeIf { (_, blockState) ->
                            runCatching { blockState.handleInitialized(false) }
                                .onFailure { LOGGER.log(Level.SEVERE, "Failed to initialize $blockState", it) }
                                .isFailure
                        }
                    
                    val event = NovaChunkLoadedEvent(pos, blockStates)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
        }
    }
    
    private fun unloadChunk(pos: ChunkPos) {
        val worldStorage = getWorldStorage(pos.world!!)
        
        // The chunk might not be loaded if it was unloaded before Nova could load it
        val chunk = worldStorage.getRegionOrNull(pos)
            ?.getChunkOrNull(pos) ?: return
        
        chunk.lock.read {
            chunk.blockStates.values.forEach { it.handleRemoved(false) }
        }
    }
    
    private fun saveWorld(world: World) {
        worlds[world]?.saveAll()
    }
    
    private fun unloadWorld(world: World) {
        if (world in worlds) { // TODO: is this if always true?
            world.loadedChunks.forEach { unloadChunk(it.pos) }
            worlds -= world
        }
    }
    
    fun getBlockStates(pos: ChunkPos, takeUninitialized: Boolean = false): Map<BlockPos, BlockState> =
        readChunk(pos) { chunk -> HashMap(chunk.blockStates) }.apply { if (!takeUninitialized) removeIf { !it.value.isInitialized } }
    
    fun getBlockState(pos: BlockPos, takeUninitialized: Boolean = false): BlockState? =
        readChunk(pos.chunkPos) { chunk -> chunk.blockStates[pos]?.takeIf { takeUninitialized || it.isInitialized } }
    
    fun setBlockState(pos: BlockPos, state: BlockState) =
        writeChunk(pos.chunkPos) { it.blockStates[pos] = state }
    
    fun removeBlockState(pos: BlockPos) =
        writeChunk(pos.chunkPos) { it.blockStates -= pos }
    
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.getOrPut(world) { WorldDataStorage(world) }
    
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
    
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        tasks += WorldUnloadTask(event.world)
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            tasks += ChunkLoadTask(event.chunk.pos)
        }
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            tasks += ChunkUnloadTask(event.chunk.pos)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            tasks += SaveWorldTask(event.world)
        }
    }
    
}