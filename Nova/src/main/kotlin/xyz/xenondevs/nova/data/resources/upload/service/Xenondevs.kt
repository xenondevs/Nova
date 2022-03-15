package xyz.xenondevs.nova.data.resources.upload.service

import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.config.JsonConfig
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.util.data.HttpMultipartRequest
import xyz.xenondevs.nova.util.data.getString
import java.io.File

object Xenondevs : UploadService {
    
    private const val API_URL = "https://api.xenondevs.xyz/nova/rp/patreon/upload"
    
    override val name = "xenondevs"
    lateinit var key: String
    
    override fun loadConfig(json: JsonConfig) {
        key = json.getString("key")
            ?: throw IllegalArgumentException("No key specified for xenondevs upload service")
    }
    
    override fun upload(file: File): String {
        println("uploading")
        val request = HttpMultipartRequest(API_URL, "PUT") { setRequestProperty("key", key) }
        request.addFormFile("pack", file, "pack.zip")
        val response = request.complete()
        val json = response.jsonResponse as JsonObject
        check(response.isSuccessful) {
            "Failed to upload pack to xenondevs: ${response.statusCode} ${json.getString("error")}." +
                "Please remember that this feature is only available for Patrons!"
        }
        val url = json.getString("url")
        checkNotNull(url) { "Server did not return a url" }
        println("uploaded!: $url")
        return url
    }
    
}
