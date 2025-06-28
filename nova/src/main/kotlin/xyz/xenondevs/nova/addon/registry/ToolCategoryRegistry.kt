package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.item.tool.ToolCategory

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface ToolCategoryRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerToolCategory(name: String): ToolCategory =
        addon.registerToolCategory(name)
    
}