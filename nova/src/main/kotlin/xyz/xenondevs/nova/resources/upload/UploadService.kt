package xyz.xenondevs.nova.resources.upload

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

/**
 * Configuration for an [UploadService].
 */
@Serializable(with = UploadServiceConfigSerializer::class)
internal sealed interface UploadServiceConfig {
    
    /**
     * Whether the upload service is enabled.
     */
    val enabled: Boolean
    
    /**
     * Creates the [UploadService] for this configuration.
     */
    fun createService(): UploadService
    
}

@Serializable
internal class DisabledUploadServiceConfig(
    override val enabled: Boolean = false
) : UploadServiceConfig {
    override fun createService() = throw UnsupportedOperationException("Cannot create a service for a disabled config")
}

internal object UploadServiceConfigSerializer : JsonContentPolymorphicSerializer<UploadServiceConfig>(UploadServiceConfig::class) {
    
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<UploadServiceConfig> {
        val obj = element.jsonObject
        val service = obj["service"]?.jsonPrimitive?.contentOrNull
            ?: return DisabledUploadServiceConfig.serializer()
        return when (service) {
            "self_host", "selfhost" -> SelfHostConfig.serializer()
            "s3", "amazon_s3" -> S3Config.serializer()
            "custom_multi_part", "custommultipart" -> CustomMultiPartConfig.serializer()
            else -> throw SerializationException("Unknown upload service: $service. Available services: self_host, s3, custom_multi_part")
        }
    }
    
}

/**
 * Uploads resource packs to somewhere and returns an HTTP download url.
 */
internal interface UploadService {
    
    /**
     * Enables this upload service.
     */
    suspend fun enable()
    
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
