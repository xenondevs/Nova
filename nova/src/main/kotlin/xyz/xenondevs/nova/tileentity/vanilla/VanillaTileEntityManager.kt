package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.pos
import java.util.*

/**
 * Manages wrappers for vanilla TileEntities
 */
@InternalInit(
    stage = InitializationStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class]
)
internal object VanillaTileEntityManager : Listener {
    
    private val tileEntityMap: MutableMap<ChunkPos, MutableMap<BlockPos, VanillaTileEntity>> =
        Collections.synchronizedMap(HashMap())
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    internal fun registerTileEntity(state: VanillaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { Collections.synchronizedMap(HashMap()) }[state.pos] = state.tileEntity
    }
    
    internal fun unregisterTileEntity(state: VanillaTileEntityState) {
        val pos = state.pos
        tileEntityMap[pos.chunkPos]?.remove(pos)
    }
    
    internal fun removeInvalidVTEs(): Int {
        val invalidPositions = ArrayList<BlockPos>()
        
        synchronized(tileEntityMap) {
            tileEntityMap.forEach { (_, blockMap) ->
                synchronized(blockMap) {
                    blockMap.forEach { (blockPos, vte) ->
                        if (!vte.meetsBlockStateRequirement())
                            invalidPositions += blockPos
                    }
                }
            }
        }
        
        invalidPositions.forEach(::handleBlockBreak)
        
        return invalidPositions.size
    }
    
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunkPos]?.get(location.pos)
    
    fun getTileEntityAt(pos: BlockPos) = tileEntityMap[pos.chunkPos]?.get(pos)
    
    fun getTileEntitiesInChunk(pos: ChunkPos): List<VanillaTileEntity> {
        val tileEntities = tileEntityMap[pos] ?: return emptyList()
        return synchronized(tileEntities) { tileEntities.values.toList() }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePlace(event: BlockPlaceEvent) {
        tryCreateVTE(event.block.pos)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val pos = event.block.pos
        val vte = getTileEntityAt(pos)
        if (vte != null) {
            if (vte.meetsBlockStateRequirement()) 
                vte.handleBlockUpdate()
             else handleBlockBreak(pos)
        } else tryCreateVTE(pos)
    }
    
    private fun tryCreateVTE(pos: BlockPos): VanillaTileEntityState? {
        // Prevent creation of vanilla tile entities for custom item service blocks
        if (CustomItemServiceManager.getBlockType(pos.block) != null)
            return null
        
        val block = pos.block
        val type = VanillaTileEntity.Type.of(block) ?: return null
        
        // prevents vanilla tile entities for hitbox blocks / solid blocks
        if (WorldDataManager.getBlockState(pos) != null) return null
        
        val blockState = VanillaTileEntityState(pos, type.id)
        WorldDataManager.setBlockState(pos, blockState)
        blockState.handleInitialized(true)
        
        return blockState
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBreak(event: BlockBreakEvent) {
        handleBlockBreak(event.block.pos)
    }
    
    private fun handleBlockBreak(pos: BlockPos) {
        val blockState = WorldDataManager.getBlockState(pos)
        if (blockState is VanillaTileEntityState) {
            WorldDataManager.removeBlockState(pos)
            blockState.handleRemoved(true)
        }
    }
    
}