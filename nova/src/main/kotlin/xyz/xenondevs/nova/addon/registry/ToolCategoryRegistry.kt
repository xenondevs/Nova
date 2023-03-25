package xyz.xenondevs.nova.addon.registry

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set

interface ToolCategoryRegistry: AddonGetter {
    
    fun toolCategory(name: String, iconGetter: (ToolTier?) -> ResourcePath): ToolCategory {
        val id = ResourceLocation(addon.description.id, name)
        val category = ToolCategory(id, iconGetter)
        
        NovaRegistries.TOOL_CATEGORY[id] = category
        return category
    }
    
}