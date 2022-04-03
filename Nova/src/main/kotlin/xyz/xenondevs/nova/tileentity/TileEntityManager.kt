package xyz.xenondevs.nova.tileentity

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.util.runAsyncTaskTimerSynchronized
import xyz.xenondevs.nova.util.runTaskTimerSynchronized
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

object TileEntityManager : ITileEntityManager, Listener {
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<BlockPos, TileEntity>>()
    val tileEntities: Sequence<TileEntity>
        get() = tileEntityMap.asSequence().flatMap { it.value.values }
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        NOVA.disableHandlers += { Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach { handleChunkUnload(it.pos) } }
        
        runTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { tileEntity -> if (tileEntity.isValid) tileEntity.handleTick() }
        }
        runAsyncTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { tileEntity -> if (tileEntity.isValid) tileEntity.handleAsyncTick() }
        }
    }
    
    fun registerTileEntity(state: NovaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { HashMap() }[state.pos] = state.tileEntity
    }
    
    fun unregisterTileEntity(state: NovaTileEntityState) {
        tileEntityMap[state.pos.chunkPos]?.remove(state.pos)
    }
    
    override fun getTileEntityAt(location: Location): TileEntity? {
        return getTileEntityAt(location, true)
    }
    
    fun getTileEntityAt(location: Location, additionalHitboxes: Boolean): TileEntity? {
        val blockState = BlockManager.getBlock(location.pos, additionalHitboxes)
        return if (blockState is NovaTileEntityState && blockState.isInitialized) blockState.tileEntity else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: ChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        handleChunkUnload(event.chunk.pos)
    }
    
    @Synchronized
    private fun handleChunkUnload(chunkPos: ChunkPos) {
        if (chunkPos in tileEntityMap) {
            val tileEntities = tileEntityMap[chunkPos]!!
            
            tileEntityMap -= chunkPos
            tileEntities.values.forEach { it.saveData() }
        }
    }
    
}
