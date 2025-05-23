package xyz.xenondevs.nova.world.block.migrator

import org.bukkit.block.data.type.Leaves
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.StructureGrowEvent
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.registerEvents

/**
 * Due to block migration of leaves, trees growing into other trees may cause persistent leaves to be created.
 * PersistentLeavesGrowthFixer makes all grown leaves non-persistent.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object PersistentLeavesGrowthFixer : Listener {
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler
    private fun handleTreeGrowth(event: StructureGrowEvent) {
        for (block in event.blocks) {
            val data = block.blockData as? Leaves
                ?: continue
            data.isPersistent = false
            block.blockData = data
        }
    }
    
}