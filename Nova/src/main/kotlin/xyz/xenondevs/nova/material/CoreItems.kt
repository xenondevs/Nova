package xyz.xenondevs.nova.material

import xyz.xenondevs.nova.item.impl.Wrench

object CoreItems {
    
    // Might be moved to a separate addon in the future
    val SPEED_UPGRADE = NovaMaterialRegistry.registerDefaultItem("speed_upgrade")
    val EFFICIENCY_UPGRADE = NovaMaterialRegistry.registerDefaultItem("efficiency_upgrade")
    val ENERGY_UPGRADE = NovaMaterialRegistry.registerDefaultItem("energy_upgrade")
    val RANGE_UPGRADE = NovaMaterialRegistry.registerDefaultItem("range_upgrade")
    val FLUID_UPGRADE = NovaMaterialRegistry.registerDefaultItem("fluid_upgrade",)
    
    val WRENCH = NovaMaterialRegistry.registerDefaultItem("wrench", Wrench)
    
    fun init() = Unit
    
}