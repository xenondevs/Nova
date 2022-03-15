package xyz.xenondevs.nova.data.resources.upload

import xyz.xenondevs.nova.data.config.JsonConfig
import java.io.File

interface UploadService {
    
    val name: String
    
    fun loadConfig(json: JsonConfig)
    
    fun upload(file: File): String
    
}