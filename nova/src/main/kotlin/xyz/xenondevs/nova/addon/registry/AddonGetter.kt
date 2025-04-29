package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION

@Deprecated(REGISTRIES_DEPRECATION)
interface AddonGetter {
    
    val addon: Addon
    
}