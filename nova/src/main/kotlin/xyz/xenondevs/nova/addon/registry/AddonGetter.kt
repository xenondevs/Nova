package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.Addon

sealed interface AddonGetter {
    
    val addon: Addon
    
}