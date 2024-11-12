package xyz.xenondevs.nova.resources.upload

import org.spongepowered.configurate.ConfigurationNode
import java.nio.file.Path

interface UploadService {
    
    val names: List<String>
    
    fun loadConfig(cfg: ConfigurationNode)
    
    suspend fun upload(file: Path): String
    
    fun disable()
    
}