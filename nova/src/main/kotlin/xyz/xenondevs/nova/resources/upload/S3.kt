package xyz.xenondevs.nova.resources.upload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import xyz.xenondevs.nova.LOGGER
import java.net.URI
import java.util.*

@SerialName("s3")
@Serializable
internal class S3Config(
    override val enabled: Boolean,
    private val endpoint: String,
    @SerialName("key_id")
    @JsonNames("keyId")
    private val keyId: String,
    @SerialName("key_secret")
    @JsonNames("keySecret")
    private val keySecret: String,
    private val region: String? = null,
    @SerialName("disable_chunked_encoding")
    @JsonNames("disableChunkedEncoding")
    private val disableChunkedEncoding: Boolean = false,
    @SerialName("force_path_style")
    @JsonNames("forcePathStyle")
    private val pathStyle: Boolean = false,
    private val bucket: String,
    private val directory: String = "",
    private val acl: String? = null,
    private val domain: String? = null,
) : UploadServiceConfig {
    
    @Suppress("HttpUrlsUsage")
    override fun createService() = S3Service(
        endpoint = endpoint.removePrefix("https://").removePrefix("http://"),
        keyId = keyId,
        keySecret = keySecret,
        region = region,
        disableChunkedEncoding = disableChunkedEncoding,
        pathStyle = pathStyle,
        bucket = bucket,
        directory = if (directory.isEmpty()) "" else if (directory.endsWith("/")) directory else "$directory/",
        acl = acl,
        domain = domain,
    )
    
}

internal class S3Service(
    private val endpoint: String,
    private val keyId: String,
    private val keySecret: String,
    private val region: String?,
    private val disableChunkedEncoding: Boolean,
    private val pathStyle: Boolean,
    private val bucket: String,
    private val directory: String,
    private val acl: String?,
    private val domain: String?,
) : UploadService {
    
    private var client: S3Client? = null
    
    override suspend fun enable() {
        LOGGER.info("Connecting to S3 endpoint $endpoint")
        client = S3Client.builder()
            .endpointOverride(URI("https://$endpoint"))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, keySecret)))
            .apply { if (region != null) region(Region.of(region)) }
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyle)
                    .chunkedEncodingEnabled(!disableChunkedEncoding)
                    .build()
            )
            .build()
    }
    
    override suspend fun disable() {
        client = null
    }
    
    override suspend fun upload(id: UUID, bin: ByteArray): String = withContext(Dispatchers.IO) {
        val req = PutObjectRequest.builder().apply {
            bucket(bucket)
            key("$directory$id.zip")
            if (acl != null)
                acl(acl)
        }.build()
        val resp = client!!.putObject(req, RequestBody.fromBytes(bin)).sdkHttpResponse()
        
        if (!resp.isSuccessful)
            throw IllegalStateException("S3 upload failed with code ${resp.statusCode()} " + resp.statusText().orElse(""))
        
        return@withContext getDownloadUrl(id)
    }
    
    private fun getDownloadUrl(id: UUID): String {
        val url = domain ?: if (pathStyle) {
            "$endpoint/$bucket"
        } else {
            "$bucket.$endpoint"
        }
        
        return "https://$url/$directory$id.zip"
    }
    
}
