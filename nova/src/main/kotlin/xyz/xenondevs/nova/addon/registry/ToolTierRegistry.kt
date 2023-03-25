package xyz.xenondevs.nova.addon.registry

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.registry.HardcodedProperties
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.set

interface ToolTierRegistry: AddonGetter {
    
    fun toolTier(name: String): ToolTier {
        val id = ResourceLocation(addon.description.id, name)
        val tier = ToolTier(id, configReloadable {
            NovaConfig["${id.namespace}:tool_levels"].getDouble(id.name)
        })
    
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
    @HardcodedProperties
    fun toolTier(name: String, level: Double) : ToolTier {
        val id = ResourceLocation(addon.description.id, name)
        val tier = ToolTier(id, provider(level))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
}