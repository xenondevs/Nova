package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos

/**
 * Manages wrappers for vanilla TileEntities
 */
@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class]
)
internal object VanillaTileEntityManager : Listener {
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePlace(event: BlockPlaceEvent) {
        tryCreateVTE(event.block.pos)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val pos = event.block.pos
        val vte = WorldDataManager.getVanillaTileEntity(pos)
        if (vte != null) {
            if (vte.meetsBlockStateRequirement()) 
                vte.handleBlockUpdate()
             else handleBlockBreak(pos)
        } else tryCreateVTE(pos)
    }
    
    private fun tryCreateVTE(pos: BlockPos) {
        // Prevent creation of vanilla tile entities for custom item service blocks
        if (CustomItemServiceManager.getBlockType(pos.block) != null)
            return
        
        val block = pos.block
        val type = VanillaTileEntity.Type.of(block)
            ?: return
        
        // prevents vanilla tile entities for hitbox blocks / solid blocks
        if (WorldDataManager.getBlockState(pos) != null)
            return
        
        val vte = type.constructor(pos, Compound())
        WorldDataManager.setVanillaTileEntity(pos, vte)
        vte.handlePlace()
    }
    
    internal fun removeInvalidVTEs(chunkPos: ChunkPos): Int {
        var count = 0
        for (vte in WorldDataManager.getVanillaTileEntities(chunkPos)) {
            if (!vte.meetsBlockStateRequirement()) {
                handleBlockBreak(vte.pos)
                count++
            }
        }
        
        return count
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBreak(event: BlockBreakEvent) {
        handleBlockBreak(event.block.pos)
    }
    
    private fun handleBlockBreak(pos: BlockPos) {
        val vte = WorldDataManager.getVanillaTileEntity(pos)
        if (vte != null) {
            WorldDataManager.setVanillaTileEntity(pos, null)
            vte.handleBreak()
        }
    }
    
}