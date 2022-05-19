package xyz.xenondevs.nova.data.resources.upload

import org.bukkit.configuration.ConfigurationSection
import java.io.File

internal interface UploadService {
    
    val name: String
    
    fun loadConfig(cfg: ConfigurationSection)
    
    suspend fun upload(file: File): String
    
    fun disable()
    
}