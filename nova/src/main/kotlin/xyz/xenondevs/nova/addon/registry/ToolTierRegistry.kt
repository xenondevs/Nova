package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.tool.ToolTier

interface ToolTierRegistry : AddonGetter {
    
    fun registerToolTier(name: String): ToolTier {
        val id = Key(addon, name)
        val tier = ToolTier(id, Configs["${id.namespace()}:tool_levels"].entry(id.value()))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
    fun registerToolTier(name: String, level: Double): ToolTier {
        val id = Key(addon, name)
        val tier = ToolTier(id, provider(level))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
}