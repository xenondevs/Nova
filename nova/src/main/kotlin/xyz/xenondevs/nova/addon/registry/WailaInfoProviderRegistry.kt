package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface WailaInfoProviderRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T> registerWailaInfoProvider(name: String, provider: WailaInfoProvider<T>): WailaInfoProvider<T> =
        addon.registerWailaInfoProvider(name, provider)
    
}