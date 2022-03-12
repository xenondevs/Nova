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
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.concurrent.runIfTrue
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.*

/**
 * Manages wrappers for vanilla TileEntities
 */
object VanillaTileEntityManager : Initializable(), Listener {
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<Location, VanillaTileEntity>>()
    private val locationCache = HashMap<Location, VanillaTileEntity>()
    private val tileEntityQueue = LinkedList<VanillaTileEntity>()
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
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
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunkPos]?.get(location)
    
    @Synchronized
    private fun createVanillaTileEntity(state: BlockState) =
        when (state) {
            is Chest -> VanillaChestTileEntity(state)
            is Furnace -> VanillaFurnaceTileEntity(state)
            is Container -> VanillaContainerTileEntity(state)
            else -> null
        }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInteract(event: PlayerInteractEvent) {
        val player = event.player
        val tileEntity = event.clickedBlock?.location?.let(::getTileEntityAt)
        if (
            tileEntity is ItemStorageVanillaTileEntity
            && !player.isSneaking
            && event.handItems.any { it.novaMaterial == CoreItems.WRENCH }
        ) {
            event.isCancelled = true
            val face = event.blockFace
            ProtectionManager.canUseBlock(player, event.item, tileEntity.location).runIfTrue {
                NetworkManager.runAsync {
                    tileEntity.itemHolder.cycleItemConfig(it, face, true)
                }
            }
        }
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
        tileEntityMap[chunk.pos] = chunkMap
        chunkMap.values.forEach(VanillaTileEntity::handleInitialized)
    }
    
    private fun handleChunkUnload(chunk: Chunk) {
        // The VanillaTileEntityManager lock should not be hold for VanillaTileEntity#handleRemoved
        synchronized(this) {
            val tileEntities = tileEntityMap.remove(chunk.pos)
            tileEntities?.forEach { (location, _) -> locationCache -= location }
            return@synchronized tileEntities
        }?.forEach { (_, tileEntity) -> tileEntity.handleRemoved(true) }
    }
    
    @Synchronized
    private fun handleTileEntityDestroy(location: Location) {
        val chunkMap = tileEntityMap[location.chunkPos]!!
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val block = event.block
        val state = block.state
        
        val tileEntity = createVanillaTileEntity(state)
        if (tileEntity != null) {
            val location = block.location
            tileEntityMap[location.chunkPos]!![location] = tileEntity
            locationCache[location] = tileEntity
            tileEntity.handleInitialized()
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleBreak(event: BlockBreakEvent) {
        val location = event.block.location
        handleBlockBreak(location)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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