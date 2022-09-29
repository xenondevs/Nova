package xyz.xenondevs.nova.data.world

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.BlockFace
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
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.concurrent.thread
import net.minecraft.world.level.Level as MojangWorld

internal object WorldDataManager : Initializable(), Listener {
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(AddonsInitializer, LegacyFileConverter, TileEntityManager, VanillaTileEntityManager, NetworkManager)
    
    private val worlds = HashMap<World, WorldDataStorage>()
    private val tasks = ConcurrentLinkedQueue<Task>() // TODO: Map RegionFile -> Queue
    
    private val pendingOrphanBlocks = Object2ObjectOpenHashMap<ChunkPos, MutableMap<BlockPos, BlockNovaMaterial>>()
    
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
    
    @Synchronized
    private fun loadChunk(pos: ChunkPos) {
        if (pos.isLoaded()) {
            val blockStates = getBlockStates(pos)
            
            runTask {
                if (pos.isLoaded()) {
                    if (pos in pendingOrphanBlocks) {
                        pendingOrphanBlocks[pos]?.forEach(::placeOrphanBlock)
                        pendingOrphanBlocks -= pos
                    }
                    ArrayList(blockStates.values).forEach { blockState ->
                        try {
                            blockState.handleInitialized(false)
                        } catch (t: Throwable) {
                            LOGGER.log(Level.SEVERE, "Failed to initialize $blockState", t)
                            blockStates -= blockState.pos
                        }
                    }
                    
                    val event = NovaChunkLoadedEvent(pos, blockStates)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
        }
    }
    
    @Synchronized
    private fun unloadChunk(pos: ChunkPos) {
        val worldStorage = getWorldStorage(pos.world!!)
        // The chunk might not be loaded if it was unloaded before Nova could load it
        worldStorage.getRegionOrNull(pos)
            ?.getChunkOrNull(pos)
            ?.blockStates?.values
            ?.forEach { it.handleRemoved(false) }
    }
    
    @Synchronized
    private fun saveWorld(world: World) {
        worlds[world]?.saveAll()
    }
    
    @Synchronized
    private fun unloadWorld(world: World) {
        if (world in worlds) { // TODO: is this if always true?
            world.loadedChunks.forEach { unloadChunk(it.pos) }
            worlds -= world
        }
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
    internal fun addOrphanBlock(world: MojangWorld, x: Int, y: Int, z: Int, material: BlockNovaMaterial) {
        val pos = BlockPos(world.world, x, y, z)
        val chunk = pos.chunkPos
        if (chunk.isLoaded()) {
            placeOrphanBlock(pos, material)
        } else {
            pendingOrphanBlocks.getOrPut(chunk, ::Object2ObjectOpenHashMap)[pos] = material
        }
    }
    
    @Synchronized
    private fun placeOrphanBlock(pos: BlockPos, material: BlockNovaMaterial) {
        val ctx = BlockPlaceContext(pos, material.clientsideProvider.get(), null, null, null, pos.below, BlockFace.UP)
        val state = material.createNewBlockState(ctx)
        setBlockState(pos, state)
        state.handleInitialized(true)
        material.novaBlock.handlePlace(state, ctx)
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
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        LegacyFileConverter.addConversionListener(event.world) {
            tasks += SaveWorldTask(event.world)
        }
    }
    
}