package xyz.xenondevs.nova.resources.upload

import org.spongepowered.configurate.ConfigurationNode
import java.io.File

interface UploadService {
    
    val names: List<String>
    
    fun loadConfig(cfg: ConfigurationNode)
    
    suspend fun upload(file: File): String
    
    fun disable()
    
}