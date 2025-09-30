package xyz.xenondevs.nova.resources.upload

import org.spongepowered.configurate.ConfigurationNode
import java.util.*

/**
 * Uploads resource packs to somewhere and returns an HTTP download url.
 */
internal interface UploadService {
    
    /**
     * The names this upload service is identified by.
     */
    val names: Set<String>
    
    /**
     * Enables this upload service based on the provided configuration [cfg].
     */
    suspend fun enable(cfg: ConfigurationNode)
    
    /**
     * Disables this upload service.
     */
    suspend fun disable()
    
    /**
     * Uploads a resource pack with the given [id] and binary data [bin],
     * then returns an HTTP download url.
     */
    suspend fun upload(id: UUID, bin: ByteArray): String
    
}