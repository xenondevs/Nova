package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface WailaToolIconProviderRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerWailaToolIconProvider(name: String, provider: WailaToolIconProvider): WailaToolIconProvider =
        addon.registerWailaToolIconProvider(name, provider)
    
}