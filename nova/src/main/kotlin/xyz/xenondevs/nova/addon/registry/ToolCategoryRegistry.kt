package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.tool.ToolCategory

interface ToolCategoryRegistry : AddonHolder {
    
    fun registerToolCategory(name: String): ToolCategory {
        val id = ResourceLocation(addon, name)
        val category = ToolCategory(id)
        
        NovaRegistries.TOOL_CATEGORY[id] = category
        return category
    }
    
}