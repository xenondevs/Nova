package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.tool.ToolCategory

interface ToolCategoryRegistry : AddonGetter {
    
    fun registerToolCategory(name: String): ToolCategory {
        val id = Key(addon, name)
        val category = ToolCategory(id)
        
        NovaRegistries.TOOL_CATEGORY[id] = category
        return category
    }
    
}