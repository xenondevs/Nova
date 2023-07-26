package xyz.xenondevs.nova.tileentity

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Location
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("DEPRECATION")
val Material?.requiresLight: Boolean
    get() = this != null && !isTransparent && isOccluding

@InternalInit(
    stage = InitializationStage.POST_WORLD,
    dependsOn = [HooksLoader::class]
)
object TileEntityManager {
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<BlockPos, TileEntity>>()
    val tileEntities: Sequence<TileEntity>
        get() = tileEntityMap.asSequence().filter { it.key.isLoaded() }.flatMap { it.value.values }
    
    @InitFun
    private fun init() {
        fun handleTick(tickHandler: (TileEntity) -> Unit) {
            val tileEntities = synchronized(this) { tileEntities.toList() }
            tileEntities.forEach { tileEntity ->
                if (tileEntity.isValid) {
                    try {
                        tickHandler.invoke(tileEntity)
                    } catch (e: Exception) {
                        LOGGER.log(Level.SEVERE, "An exception occurred while ticking $tileEntity", e)
                    }
                }
            }
        }
        
        runTaskTimer(0, 1) { handleTick(TileEntity::handleTick) }
        // TODO: multiple threads for async ticks
        Executors.newSingleThreadScheduledExecutor(ThreadFactoryBuilder().setNameFormat("Nova Async TileEntityManager").build())
            .scheduleAtFixedRate({ handleTick(TileEntity::handleAsyncTick) }, 0, 50, TimeUnit.MILLISECONDS)
    }
    
    @Synchronized
    internal fun registerTileEntity(state: NovaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { HashMap() }[state.pos] = state.tileEntity
    }
    
    @Synchronized
    internal fun unregisterTileEntity(state: NovaTileEntityState) {
        tileEntityMap[state.pos.chunkPos]?.remove(state.pos)
    }
    
    fun getTileEntity(location: Location): TileEntity? {
        return getTileEntity(location, true)
    }
    
    fun getTileEntity(location: Location, additionalHitboxes: Boolean): TileEntity? {
        return getTileEntity(location.pos, additionalHitboxes)
    }
    
    fun getTileEntity(pos: BlockPos, additionalHitboxes: Boolean = true): TileEntity? {
        val blockState = BlockManager.getBlockState(pos, additionalHitboxes)
        return if (blockState is NovaTileEntityState) blockState.tileEntity else null
    }
    
    @Synchronized
    fun getTileEntitiesInChunk(chunkPos: ChunkPos) = tileEntityMap[chunkPos]?.values?.toList() ?: emptyList()
    
}
