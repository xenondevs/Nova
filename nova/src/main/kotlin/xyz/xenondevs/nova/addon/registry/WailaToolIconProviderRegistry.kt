package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.util.set

interface WailaToolIconProviderRegistry : AddonGetter {
    
    fun <T> registerWailaToolIconProvider(name: String, provider: WailaToolIconProvider): WailaToolIconProvider {
        val id = addon.description.id + ":" + name
        
        NovaRegistries.WAILA_TOOL_ICON_PROVIDER[id] = provider
        return provider
    }
    
}