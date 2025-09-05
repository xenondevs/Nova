package xyz.xenondevs.nova.resources.upload.service

import org.spongepowered.configurate.ConfigurationNode
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.util.StringUtils
import xyz.xenondevs.nova.util.addSuffix
import java.net.URI
import java.nio.file.Path

internal object S3 : UploadService {
    
    override val names = listOf("amazon_s3", "s3")
    
    private var client: S3Client? = null
    private lateinit var bucket: String
    private lateinit var directory: String
    private lateinit var urlFormat: String
    
    override fun loadConfig(cfg: ConfigurationNode) {
        val endpoint = cfg.node("endpoint").string?.removePrefix("https://")?.removePrefix("http://")
            ?: throw IllegalArgumentException("S3 endpoint is not specified")
        val endpointURI = URI("https://$endpoint")
        val keyId = cfg.node("key_id").string
            ?: throw IllegalArgumentException("S3 key_id is not specified")
        val keySecret = cfg.node("key_secret").string
            ?: throw IllegalArgumentException("S3 key_secret is not specified")
        val credentials = AwsBasicCredentials.create(keyId, keySecret)
        
        val region = cfg.node("region")?.string?.let(Region::of)
            ?: throw IllegalArgumentException("S3 region is not specified. Regions available by default are: " +
                Region.regions().joinToString(", ", transform = Region::id))
        
        val forcePathStyle = cfg.node("force_path_style").getBoolean(false)
        val disableChunkedEncoding = cfg.node("disable_chunked_encoding").getBoolean(false)

        val urlStyle = cfg.node("url_style").string?.lowercase()
            .takeIf { it in listOf("path", "vhost") } ?: "path"
        
        LOGGER.info("Connecting to S3 endpoint $endpoint")

        this.client = S3Client.builder()
            .endpointOverride(endpointURI)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(region)
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(forcePathStyle)
                    .chunkedEncodingEnabled(!disableChunkedEncoding)
                    .build()
            )
            .build()
        this.bucket = cfg.node("bucket").string
            ?: throw IllegalArgumentException("S3 bucket is not specified")
        if (this.client!!.listBuckets().buckets().none { it.name() == bucket })
            throw IllegalArgumentException("S3 bucket $bucket not found")
        
        this.directory = (cfg.node("directory").string?.addSuffix("/") ?: "")
        this.urlFormat = when (urlStyle) {
            "path" -> "https://$endpoint/$bucket/$directory%s"
            "vhost" -> "https://$bucket.$endpoint/$directory%s"
            else -> throw IllegalStateException("Unreachable")
        }
    }
    
    override suspend fun upload(file: Path): String {
        val key = StringUtils.randomString(5)
        val req = PutObjectRequest.builder()
            .bucket(bucket)
            .key(directory + key)
            .build()
        val resp = client!!.putObject(req, file).sdkHttpResponse()
        
        if (!resp.isSuccessful)
            throw IllegalStateException("S3 upload failed with code ${resp.statusCode()} " + resp.statusText().orElse(""))

        val lastUrl: String? = PermanentStorage.retrieve("lastS3Url")
        val lastBucket: String? = PermanentStorage.retrieve("lastS3Bucket")
        val lastKey: String? = PermanentStorage.retrieve("lastS3Key")
        if (lastUrl != null && lastUrl.startsWith(urlFormat.dropLast(2 + directory.length))) {
            val delReq = DeleteObjectRequest.builder()
                .bucket(lastBucket)
                .key(lastKey)
                .build()
            
            val delResp = client!!.deleteObject(delReq)
            if (!delResp.sdkHttpResponse().isSuccessful)
                LOGGER.warn("S3 delete of old resourcepack failed with code ${delResp.sdkHttpResponse().statusCode()} "
                    + delResp.sdkHttpResponse().statusText().orElse(""))
        }
        
        val url = urlFormat.format(key)
        PermanentStorage.store("lastS3Url", url)
        PermanentStorage.store("lastS3Bucket", bucket)
        PermanentStorage.store("lastS3Key", key)
        return url
    }
    
    override fun disable() {
        client = null
    }
}