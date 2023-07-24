package xyz.xenondevs.nova.data.resources.upload.service

import com.google.gson.JsonObject
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Zip
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.util.data.http.BinaryBufferedBody
import java.io.File

internal object Xenondevs : UploadService {
    
    private const val API_URL = "https://api.xenondevs.xyz/nova/rp/patreon/upload"
    
    override val names = listOf("xenondevs")
    private lateinit var key: String
    
    override fun loadConfig(cfg: ConfigurationNode) {
        key = cfg.node("key").string
            ?: throw IllegalArgumentException("No key specified for xenondevs upload service")
    }
    
    override suspend fun upload(file: File): String {
        val json = HTTP_CLIENT.preparePut(API_URL) {
            header("key", key)
            setBody(BinaryBufferedBody(file.inputStream(), contentType = Zip))
        }.execute { response ->
            val json = response.body<JsonObject>()
            check(response.status.isSuccess()) {
                "Failed to upload pack to xenondevs: ${response.status} ${json.getStringOrNull("error")}." +
                    "Please remember that this feature is only available for Patrons!"
            }
            return@execute json
        }
        val url = json.getStringOrNull("url")
        checkNotNull(url) { "Server did not return a url" }
        return url
    }
    
    override fun disable() = Unit
    
}
