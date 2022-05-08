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
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.thread

internal object WorldDataManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn: Set<Initializable> = setOf(VanillaTileEntityManager, NetworkManager, AddonsInitializer)
    
    private val worlds = HashMap<World, WorldDataStorage>()
    private val tasks = ConcurrentLinkedQueue<Task>() // TODO: Map RegionFile -> Queue
    
    override fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        tasks += Bukkit.getWorlds().flatMap { world -> world.loadedChunks.map { ChunkLoadTask(it.pos) } }
        NOVA.disableHandlers += { Bukkit.getWorlds().forEach(::saveWorld) }
        
        thread(name = "Nova WorldDataManager", isDaemon = true) { // TODO: Use Phaser instead of Thread.sleep
            while (NOVA.isEnabled) {
                while (tasks.isNotEmpty()) {
                    when(val task = tasks.poll()) {
                        is ChunkLoadTask -> loadChunk(task.pos)
                        is ChunkUnloadTask -> unloadChunk(task.pos)
                        is SaveWorldTask -> saveWorld(task.world)
                    }
                }
                Thread.sleep(50)
            }
        }
    }
    
    @Synchronized
    private fun loadChunk(pos: ChunkPos) {
        if (pos.isLoaded()) {
            val blockStates = getBlockStates(pos)
            
            runTask {
                if (pos.isLoaded()) {
                    blockStates.removeIf { (_, blockState) ->
                        try {
                            blockState.handleInitialized(false)
                        } catch (e: Exception) {
                            LOGGER.log(Level.SEVERE, "Failed to initialize $blockState", e)
                            return@removeIf true
                        }
                        return@removeIf false
                    }
                    
                    val event = NovaChunkLoadedEvent(pos, blockStates)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
        }
    }
    
    @Synchronized
    private fun unloadChunk(pos: ChunkPos) {
        val region = getRegion(pos)
        val chunk = region.getChunk(pos)
        region.save(chunk)
        region.chunks[chunk.packedCoords.toInt()] = null
    }
    
    @Synchronized
    private fun saveWorld(world: World) {
        LOGGER.info("Saving world ${world.name}...")
        worlds[world]?.saveAll()
    }
    
    @Synchronized
    fun getBlockStates(pos: ChunkPos): MutableMap<BlockPos, BlockState> = getChunk(pos).blockStates
    
    @Synchronized
    fun getBlockState(pos: BlockPos): BlockState? = getChunk(pos.chunkPos).blockStates[pos]
    
    @Synchronized
    fun setBlockState(pos: BlockPos, state: BlockState) {
        getChunk(pos.chunkPos).blockStates[pos] = state
    }
    
    @Synchronized
    fun removeBlockState(pos: BlockPos) {
        getChunk(pos.chunkPos).blockStates -= pos
    }
    
    @Synchronized
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.getOrPut(world) { WorldDataStorage(world) }
    
    @Synchronized
    private fun getRegion(pos: ChunkPos): RegionFile =
        getWorldStorage(pos.world!!).getRegion(pos)
    
    @Synchronized
    private fun getChunk(pos: ChunkPos): RegionChunk =
        getWorldStorage(pos.world!!).getRegion(pos).getChunk(pos)
    
    @Synchronized
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        worlds -= event.world
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        tasks += ChunkLoadTask(event.chunk.pos)
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        tasks += ChunkUnloadTask(event.chunk.pos)
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        tasks += SaveWorldTask(event.world)
    }
    
}