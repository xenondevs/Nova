package xyz.xenondevs.nova.material

import xyz.xenondevs.nova.item.impl.Wrench

object CoreItems {
    
    // Might be moved to a separate addon in the future
    val SPEED_UPGRADE = NovaMaterialRegistry.registerDefaultCoreItem("speed_upgrade")
    val EFFICIENCY_UPGRADE = NovaMaterialRegistry.registerDefaultCoreItem("efficiency_upgrade")
    val ENERGY_UPGRADE = NovaMaterialRegistry.registerDefaultCoreItem("energy_upgrade")
    val RANGE_UPGRADE = NovaMaterialRegistry.registerDefaultCoreItem("range_upgrade")
    val FLUID_UPGRADE = NovaMaterialRegistry.registerDefaultCoreItem("fluid_upgrade",)
    
    val WRENCH = NovaMaterialRegistry.registerDefaultCoreItem("wrench", Wrench)
    
    fun init() = Unit
    
}