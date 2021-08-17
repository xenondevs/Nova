package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Furnace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.chunkPos
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.*

/**
 * Manages wrappers for vanilla TileEntities
 */
object VanillaTileEntityManager : Listener {
    
    private val tileEntityMap = HashMap<Chunk, HashMap<Location, VanillaTileEntity>>()
    private val locationCache = HashMap<Location, VanillaTileEntity>()
    private val tileEntityQueue = LinkedList<VanillaTileEntity>()
    
    fun init() {
        LOGGER.info("Initializing VanillaTileEntityManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(::handleChunkLoad)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(::handleChunkUnload) }
        
        // Sometimes, blocks get removed without calling the BlockPhysicsEvent.
        // For example, WorldEdit uses NMS calls to prevent block updates.
        // This timer checks repeatedly if tile entities have been removed and deletes their
        // VanillaTileEntity wrappers accordingly.
        runTaskTimer(0, 3) {
            synchronized(this) {
                if (tileEntityQueue.isNotEmpty()) {
                    val tileEntity = tileEntityQueue.poll()
                    
                    val block = tileEntity.block
                    val blockLocation = tileEntity.block.location
                    val chunkPos = blockLocation.chunkPos
                    
                    if (blockLocation.world!!.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                        if (tileEntity.type != block.type) {
                            // the block type has changed
                            handleBlockBreak(blockLocation)
                        }
                    }
                }
            }
        }
        
        // try to re-fill the queue every 30s
        runAsyncTaskTimer(0, 20 * 30) {
            synchronized(this) {
                if (tileEntityQueue.isEmpty()) {
                    locationCache.values.forEach(tileEntityQueue::add)
                }
            }
        }
    }
    
    @Synchronized
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunk]?.get(location)
    
    @Synchronized
    private fun createVanillaTileEntity(state: BlockState) =
        when (state) {
            is Chest -> VanillaChestTileEntity(state)
            is Furnace -> VanillaFurnaceTileEntity(state)
            is Container -> VanillaContainerTileEntity(state)
            else -> null
        }
    
    @Synchronized
    private fun handleChunkLoad(chunk: Chunk) {
        val chunkMap = HashMap<Location, VanillaTileEntity>()
        chunk.tileEntities.forEach { state ->
            val tileEntity = createVanillaTileEntity(state)
            if (tileEntity != null) {
                val location = state.location
                chunkMap[location] = tileEntity
                locationCache[location] = tileEntity
            }
        }
        tileEntityMap[chunk] = chunkMap
        chunkMap.values.forEach(VanillaTileEntity::handleInitialized)
    }
    
    @Synchronized
    private fun handleChunkUnload(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk]
        tileEntityMap.remove(chunk)
        tileEntities?.forEach { (location, tileEntity) ->
            locationCache -= location
            tileEntity.handleRemoved(unload = true)
        }
    }
    
    @Synchronized
    private fun handleTileEntityDestroy(location: Location) {
        val chunkMap = tileEntityMap[location.chunk]!!
        val tileEntity = chunkMap[location]!!
        chunkMap -= location
        locationCache -= location
        
        tileEntity.handleRemoved(false)
    }
    
    @Synchronized
    private fun handleBlockBreak(location: Location) {
        if (locationCache.containsKey(location))
            handleTileEntityDestroy(location)
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val block = event.block
        val state = block.state
        
        val tileEntity = createVanillaTileEntity(state)
        if (tileEntity != null) {
            val location = block.location
            tileEntityMap[location.chunk]!![location] = tileEntity
            locationCache[location] = tileEntity
            tileEntity.handleInitialized()
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        handleBlockBreak(location)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePhysics(event: BlockPhysicsEvent) {
        val location = event.block.location
        if (event.block.type == Material.AIR) handleBlockBreak(location)
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    @EventHandler
    fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk)
    }
    
}