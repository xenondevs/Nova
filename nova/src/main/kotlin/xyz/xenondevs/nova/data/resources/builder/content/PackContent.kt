package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.data.resources.builder.AssetPack

internal interface PackContent {
    
    fun addFromPack(pack: AssetPack)
    
    fun write()
    
}