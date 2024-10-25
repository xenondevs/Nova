package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.util.set

interface WailaInfoProviderRegistry : AddonHolder {
    
    fun <T> registerWailaInfoProvider(name: String, provider: WailaInfoProvider<T>): WailaInfoProvider<T> {
        val id = addon.id + ":" + name
        
        NovaRegistries.WAILA_INFO_PROVIDER[id] = provider
        return provider
    }
    
}