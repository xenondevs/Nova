package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.item.tool.ToolTier

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface ToolTierRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerToolTier(name: String): ToolTier=
        addon.registerToolTier(name)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerToolTier(name: String, level: Double): ToolTier =
        addon.registerToolTier(name, level)
    
}