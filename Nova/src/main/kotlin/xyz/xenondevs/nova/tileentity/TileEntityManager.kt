package xyz.xenondevs.nova.tileentity

import org.bukkit.Location
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.runAsyncTaskTimerSynchronized
import xyz.xenondevs.nova.util.runTaskTimerSynchronized
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import java.util.logging.Level
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

object TileEntityManager : Initializable(), ITileEntityManager {
    
    override val inMainThread = true
    override val dependsOn = setOf(CustomItemServiceManager)
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<BlockPos, TileEntity>>()
    val tileEntities: Sequence<TileEntity>
        get() = tileEntityMap.asSequence().filter { it.key.isLoaded() }.flatMap { it.value.values }
    
    override fun init() {
        fun handleTick(tileEntity: TileEntity, tickHandler: (TileEntity) -> Unit) {
            if (tileEntity.isValid) {
                try {
                    tickHandler.invoke(tileEntity)
                } catch (e: Exception) {
                    LOGGER.log(Level.SEVERE, "An exception occurred while ticking $tileEntity", e)
                }
            }
        }
        
        runTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { handleTick(it, TileEntity::handleTick) }
        }
        runAsyncTaskTimerSynchronized(this, 0, 1) {
            tileEntities.toList().forEach { handleTick(it, TileEntity::handleAsyncTick) }
        }
    }
    
    @Synchronized
    internal fun registerTileEntity(state: NovaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { HashMap() }[state.pos] = state.tileEntity
    }
    
    @Synchronized
    internal fun unregisterTileEntity(state: NovaTileEntityState) {
        tileEntityMap[state.pos.chunkPos]?.remove(state.pos)
    }
    
    override fun getTileEntity(location: Location): TileEntity? {
        return getTileEntity(location, true)
    }
    
    fun getTileEntity(location: Location, additionalHitboxes: Boolean): TileEntity? {
        return getTileEntity(location.pos, additionalHitboxes)
    }
    
    fun getTileEntity(pos: BlockPos, additionalHitboxes: Boolean = true): TileEntity? {
        val blockState = BlockManager.getBlock(pos, additionalHitboxes)
        return if (blockState is NovaTileEntityState && blockState.isInitialized) blockState.tileEntity else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: ChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
}
