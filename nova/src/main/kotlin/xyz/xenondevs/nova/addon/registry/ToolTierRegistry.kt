package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.registry.HardcodedProperties
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.set

interface ToolTierRegistry: AddonGetter {
    
    fun registerToolTier(name: String): ToolTier {
        val id = ResourceLocation(addon, name)
        val tier = ToolTier(id, Configs["${id.namespace}:tool_levels"].entry(id.name))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
    @HardcodedProperties
    fun registerToolTier(name: String, level: Double) : ToolTier {
        val id = ResourceLocation(addon, name)
        val tier = ToolTier(id, provider(level))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
}