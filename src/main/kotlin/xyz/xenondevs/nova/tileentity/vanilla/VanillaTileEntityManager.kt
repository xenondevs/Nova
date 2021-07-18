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
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.runTaskTimer
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages wrappers for vanilla TileEntities
 */
object VanillaTileEntityManager : Listener {
    
    private val tileEntityMap = ConcurrentHashMap<Chunk, ConcurrentHashMap<Location, VanillaTileEntity>>()
    private val locationCache = HashSet<Location>()
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(::handleChunkLoad)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(::handleChunkUnload) }
        
        runTaskTimer(0, 1200) {
            // In some special cases no event is called when replacing a block. So we check for air blocks every minute.
            tileEntityMap.forEach { (chunk, tileEntities) ->
                val chunkTileEntities = chunk.tileEntities.mapTo(HashSet()) { it.location }
                val iterator = tileEntities.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val location = entry.key
                    if (!chunkTileEntities.contains(location)) {
                        iterator.remove()
                        locationCache -= location
                        entry.value.handleRemoved(false)
                    }
                }
            }
        }
    }
    
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunk]?.get(location)
    
    private fun handleChunkLoad(chunk: Chunk) {
        val chunkMap = ConcurrentHashMap<Location, VanillaTileEntity>()
        chunk.tileEntities.forEach { state ->
            val tileEntity = getVanillaTileEntity(state)
            if (tileEntity != null) {
                val location = state.location
                chunkMap[location] = tileEntity
                locationCache += location
            }
        }
        tileEntityMap[chunk] = chunkMap
        chunkMap.values.forEach(VanillaTileEntity::handleInitialized)
    }
    
    private fun handleChunkUnload(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk]
        tileEntityMap.remove(chunk)
        tileEntities?.forEach { (location, tileEntity) ->
            locationCache -= location
            tileEntity.handleRemoved(unload = true)
        }
    }
    
    private fun handleTileEntityDestroy(location: Location) {
        val chunkMap = tileEntityMap[location.chunk]!!
        val tileEntity = chunkMap[location]!!
        chunkMap -= location
        locationCache -= location
        
        tileEntity.handleRemoved(false)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val block = event.block
        val state = block.state
        
        val tileEntity = getVanillaTileEntity(state)
        if (tileEntity != null) {
            val location = block.location
            tileEntityMap[location.chunk]!![location] = tileEntity
            locationCache += location
            tileEntity.handleInitialized()
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        if (location in locationCache) handleTileEntityDestroy(location)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePhysics(event: BlockPhysicsEvent) {
        val location = event.block.location
        if (location in locationCache && Material.AIR == event.block.type) {
            handleTileEntityDestroy(location)
        }
    }
    
    @EventHandler
    fun handleChunkLoad(event: ChunkLoadEvent) {
        handleChunkLoad(event.chunk)
    }
    
    @EventHandler
    fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk)
    }
    
    private fun getVanillaTileEntity(state: BlockState) =
        when (state) {
            is Chest -> VanillaChestTileEntity(state)
            is Furnace -> VanillaFurnaceTileEntity(state)
            is Container -> VanillaContainerTileEntity(state)
            else -> null
        }
    
}