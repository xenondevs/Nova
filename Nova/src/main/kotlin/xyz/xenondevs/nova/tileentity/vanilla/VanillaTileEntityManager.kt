package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.pos

/**
 * Manages wrappers for vanilla TileEntities
 */
object VanillaTileEntityManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    private val tileEntityMap = HashMap<ChunkPos, HashMap<BlockPos, VanillaTileEntity>>()
    
    override fun init() {
        LOGGER.info("Initializing VanillaTileEntityManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @Synchronized
    internal fun registerTileEntity(state: VanillaTileEntityState) {
        tileEntityMap.getOrPut(state.pos.chunkPos) { HashMap() }[state.pos] = state.tileEntity
    }
    
    @Synchronized
    internal fun unregisterTileEntity(state: VanillaTileEntityState) {
        val pos = state.pos
        tileEntityMap[pos.chunkPos]?.remove(pos)
    }
    
    @Synchronized
    fun getTileEntityAt(location: Location) = tileEntityMap[location.chunkPos]?.get(location.pos)
    
    @Synchronized
    fun getTileEntityAt(pos: BlockPos) = tileEntityMap[pos.chunkPos]?.get(pos)
    
    @Synchronized
    fun getTileEntitiesInChunk(pos: ChunkPos) = tileEntityMap[pos]?.values?.toList() ?: emptyList()
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePlace(event: BlockPlaceEvent) {
        tryCreateVTE(event.block.pos)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val block = event.block
        if (block.type == Material.AIR) {
            handleBlockBreak(block.pos)
        } else {
            val pos = block.pos
            val vte = getTileEntityAt(pos)
            if (vte != null)
                vte.handleBlockUpdate()
            else tryCreateVTE(pos)
        }
    }
    
    @Synchronized
    private fun tryCreateVTE(pos: BlockPos): VanillaTileEntityState? {
        val block = pos.block
        val type = VanillaTileEntity.Type.of(block) ?: return null
        
        val blockState = VanillaTileEntityState(pos, type.id)
        WorldDataManager.setBlockState(pos, blockState)
        blockState.handleInitialized(true)
        
        return blockState
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBreak(event: BlockBreakEvent) {
        handleBlockBreak(event.block.pos)
    }
    
    @Synchronized
    private fun handleBlockBreak(pos: BlockPos) {
        val blockState = WorldDataManager.getBlockState(pos)
        if (blockState is VanillaTileEntityState) {
            WorldDataManager.removeBlockState(pos)
            blockState.handleRemoved(true)
        }
    }
    
}