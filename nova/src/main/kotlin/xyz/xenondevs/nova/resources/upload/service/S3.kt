package xyz.xenondevs.nova.resources.upload.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spongepowered.configurate.ConfigurationNode
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.util.addSuffix
import java.net.URI
import java.util.*
import kotlin.properties.Delegates

internal object S3 : UploadService {
    
    override val names = setOf("s3", "amazon_s3")
    
    private var client: S3Client? = null
    private var pathStyle by Delegates.notNull<Boolean>()
    private lateinit var endpoint: String
    private lateinit var bucket: String
    private lateinit var directory: String // includes trailing slash
    private var acl: String? = null
    private var domain : String? = null

    @Suppress("HttpUrlsUsage")
    override suspend fun enable(cfg: ConfigurationNode) {
        endpoint = cfg.node("endpoint").string?.removePrefix("https://")?.removePrefix("http://")
            ?: throw IllegalArgumentException("S3 endpoint is not specified")
        val keyId = cfg.node("key_id").string
            ?: throw IllegalArgumentException("S3 key_id is not specified")
        val keySecret = cfg.node("key_secret").string
            ?: throw IllegalArgumentException("S3 key_secret is not specified")
        val region = cfg.node("region")?.string?.let(Region::of)
        val disableChunkedEncoding = cfg.node("disable_chunked_encoding").getBoolean(false)
        
        pathStyle = cfg.node("force_path_style").getBoolean(false)
        bucket = cfg.node("bucket").string
            ?: throw IllegalArgumentException("S3 bucket is not specified")
        directory = (cfg.node("directory").string?.addSuffix("/") ?: "")
        acl = cfg.node("acl").string
        domain = cfg.node("domain").string

        LOGGER.info("Connecting to S3 endpoint $endpoint")
        client = S3Client.builder()
            .endpointOverride(URI("https://$endpoint"))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, keySecret)))
            .region(region)
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
        val domain = if (domain != null) {
            this.domain
        } else if (pathStyle) {
            "https://$endpoint/$bucket"
        } else {
            "https://$bucket.$endpoint"
        }

        return "$domain/$directory$id.zip"
    }
    
}