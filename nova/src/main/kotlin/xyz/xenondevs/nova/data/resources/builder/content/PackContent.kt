package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.addon.assets.AssetPack

internal interface PackContent {
    
    fun addFromPack(pack: AssetPack)
    
    fun write()
    
}