package xyz.xenondevs.nova.tileentity

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.Listener
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
        
        runTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { tileEntity -> if (tileEntity.isValid) tileEntity.handleTick() }
        }
        runAsyncTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { tileEntity -> if (tileEntity.isValid) tileEntity.handleAsyncTick() }
        }
    }
    
    internal fun registerTileEntity(state: NovaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { HashMap() }[state.pos] = state.tileEntity
    }
    
    internal fun unregisterTileEntity(state: NovaTileEntityState) {
        tileEntityMap[state.pos.chunkPos]?.remove(state.pos)
    }
    
    override fun getTileEntityAt(location: Location): TileEntity? {
        return getTileEntityAt(location, true)
    }
    
    fun getTileEntityAt(location: Location, additionalHitboxes: Boolean): TileEntity? {
        return getTileEntityAt(location.pos, additionalHitboxes)
    }
    
    fun getTileEntityAt(pos: BlockPos, additionalHitboxes: Boolean = true): TileEntity? {
        val blockState = BlockManager.getBlock(pos, additionalHitboxes)
        return if (blockState is NovaTileEntityState && blockState.isInitialized) blockState.tileEntity else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: ChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
}
