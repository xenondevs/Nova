package xyz.xenondevs.nova.material

import xyz.xenondevs.nova.item.impl.WrenchBehavior

object CoreItems {
    
    // Might be moved to a separate addon in the future
    val SPEED_UPGRADE = NovaMaterialRegistry.registerCoreItem("speed_upgrade")
    val EFFICIENCY_UPGRADE = NovaMaterialRegistry.registerCoreItem("efficiency_upgrade")
    val ENERGY_UPGRADE = NovaMaterialRegistry.registerCoreItem("energy_upgrade")
    val RANGE_UPGRADE = NovaMaterialRegistry.registerCoreItem("range_upgrade")
    val FLUID_UPGRADE = NovaMaterialRegistry.registerCoreItem("fluid_upgrade")
    
    val WRENCH = NovaMaterialRegistry.registerCoreItem("wrench", WrenchBehavior)
    
    fun init() = Unit
    
}