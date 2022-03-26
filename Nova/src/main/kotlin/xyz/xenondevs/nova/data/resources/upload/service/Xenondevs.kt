package xyz.xenondevs.nova.data.resources.upload.service

import com.google.gson.JsonObject
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Zip
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.data.config.JsonConfig
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.http.BinaryBufferedBody
import java.io.File

object Xenondevs : UploadService {
    
    private const val API_URL = "https://api.xenondevs.xyz/nova/rp/patreon/upload"
    
    override val name = "xenondevs"
    lateinit var key: String
    
    override fun loadConfig(json: JsonConfig) {
        key = json.getString("key")
            ?: throw IllegalArgumentException("No key specified for xenondevs upload service")
    }
    
    override suspend fun upload(file: File): String {
        println("uploading")
        val json = HTTP_CLIENT.put<HttpStatement>(API_URL) {
            header("key", key)
            body = BinaryBufferedBody(file.inputStream(), contentType = Zip)
        }.execute { response ->
            val json = response.receive<JsonObject>()
            check(response.status.isSuccess()) {
                "Failed to upload pack to xenondevs: ${response.status} ${json.getString("error")}." +
                    "Please remember that this feature is only available for Patrons!"
            }
            return@execute json
        }
        val url = json.getString("url")
        checkNotNull(url) { "Server did not return a url" }
        println("uploaded!: $url")
        return url
    }
    
}
