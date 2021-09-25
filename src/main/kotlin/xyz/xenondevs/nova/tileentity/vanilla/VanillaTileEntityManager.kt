package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.chunkPos
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.armorstand.AsyncChunkPos
import xyz.xenondevs.nova.world.armorstand.pos
import java.util.*

/**
 * Manages wrappers for vanilla TileEntities
 */
object VanillaTileEntityManager : Listener {
    
    private val tileEntityMap = HashMap<AsyncChunkPos, HashMap<Location, VanillaTileEntity>>()
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
    
    @Synchronized
    private fun handleChunkUnload(chunk: Chunk) {
        val tileEntities = tileEntityMap[chunk.pos]
        tileEntityMap.remove(chunk.pos)
        tileEntities?.forEach { (location, tileEntity) ->
            locationCache -= location
            tileEntity.handleRemoved(unload = true)
        }
    }
    
    @Synchronized
    private fun handleTileEntityDestroy(location: Location) {
        val chunkMap = tileEntityMap[location.chunk.pos]!!
        val tileEntity = chunkMap[location]!!
        chunkMap -= location
        locationCache -= location
        
        if (tileEntity is VanillaChestTileEntity) checkForBrokenDoubleChest(tileEntity.block)
        tileEntity.handleRemoved(false)
    }
    
    @Synchronized
    private fun handleBlockBreak(location: Location) {
        if (locationCache.containsKey(location))
            handleTileEntityDestroy(location)
    }
    
    @Synchronized
    @EventHandler
    fun handleWorldSave(event: WorldSaveEvent) {
        tileEntityMap.values.asSequence()
            .flatMap { it.values }
            .filterIsInstance<ItemStorageVanillaTileEntity>()
            .forEach { it.itemHolder.saveData() }
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handlePlace(event: BlockPlaceEvent) {
        val block = event.block
        val state = block.state
        
        val tileEntity = createVanillaTileEntity(state)
        if (tileEntity != null) {
            val location = block.location
            tileEntityMap[location.chunk.pos]!![location] = tileEntity
            locationCache[location] = tileEntity
            tileEntity.handleInitialized()
            
            runTaskLater(1) { checkForPlacedDoubleChest(block) }
        }
    }
    
    @Synchronized
    private fun checkForPlacedDoubleChest(block: Block) {
        val state = block.state
        if (state is Chest) {
            val holder = state.inventory.holder
            if (holder is DoubleChest)
                getOtherChest(block, holder).handleChestStateChange()
        }
    }
    
    @Synchronized
    private fun checkForBrokenDoubleChest(block: Block) {
        // called when the double chest is about to be broken
        
        val state = block.state as Chest
        val holder = state.inventory.holder
        if (holder is DoubleChest) {
            val otherChest = getOtherChest(block, holder)
            runTaskLater(1) { synchronized(this) { otherChest.handleChestStateChange() } }
        }
    }
    
    @Synchronized
    private fun getOtherChest(block: Block, holder: DoubleChest): VanillaChestTileEntity {
        val selfLocation = block.location
        
        val left = holder.leftSide as Chest
        val right = holder.rightSide as Chest
        
        val otherLocation = if (left.location == selfLocation) right.location else left.location
        return getTileEntityAt(otherLocation) as VanillaChestTileEntity
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