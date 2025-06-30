package xyz.xenondevs.nova.resources.upload

import org.spongepowered.configurate.ConfigurationNode
import java.util.*

internal interface UploadService {
    
    val names: Set<String>
    
    suspend fun enable(cfg: ConfigurationNode)
    
    suspend fun disable()
    
    suspend fun upload(id: UUID, bin: ByteArray): String
    
}