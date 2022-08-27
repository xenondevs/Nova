package xyz.xenondevs.novagradle.util

import org.gradle.api.Project
import xyz.xenondevs.renderer.model.resource.ResourcePack
import java.io.InputStream

class AddonResourcePack(private val project: Project, private val addonId: String) : ResourcePack {
    
    override fun getResourceStream(path: String): InputStream? {
        if (!path.startsWith("assets/$addonId/"))
            return null
        
        return project.file("src/main/resources/assets/" + path.substringAfter("assets/$addonId/")).inputStream()
    }
    
}