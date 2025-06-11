package xyz.xenondevs.novagradle.util

import xyz.xenondevs.renderer.model.resource.ResourcePack
import java.io.File
import java.io.InputStream

class AddonResourcePack(private val resources: File, private val addonId: String) : ResourcePack {
    
    override fun getResourceStream(path: String): InputStream? {
        if (!path.startsWith("assets/$addonId/"))
            return null
        
        return File(resources, "assets/" + path.substringAfter("assets/$addonId/")).inputStream()
    }
    
}