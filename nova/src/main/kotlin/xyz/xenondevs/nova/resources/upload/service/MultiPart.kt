package xyz.xenondevs.nova.resources.upload.service

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.resources.upload.UploadService
import java.util.*

internal abstract class MultiPart : UploadService {
    
    protected abstract val url: String
    protected abstract val filePartName: String
    protected abstract val urlRegex: Regex
    protected abstract val extraParams: Map<String, String>
    
    override suspend fun upload(id: UUID, bin: ByteArray): String {
        val response = HTTP_CLIENT.submitFormWithBinaryData(url, formData {
            append(filePartName, bin, Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Zip)
                append(HttpHeaders.ContentDisposition, "filename=\"$id.zip\"")
            })
            extraParams.forEach(::append)
        })
        val responseString = response.body<String>()
        require(response.status == HttpStatusCode.OK) { "Upload failed with status ${response.status} and response $responseString" }
        return urlRegex.find(responseString)?.groupValues?.getOrNull(1)
            ?: throw IllegalArgumentException("No url matching $urlRegex found in response: $responseString")
    }
    
    override suspend fun disable() = Unit
    
}

internal object CustomMultiPart : MultiPart() {
    
    override val names = setOf("custom_multi_part", "custommultipart")
    
    override lateinit var url: String
    override lateinit var filePartName: String
    override lateinit var urlRegex: Regex
    override val extraParams = HashMap<String, String>()
    
    override suspend fun enable(cfg: ConfigurationNode) {
        url = cfg.node("url").string
            ?: throw IllegalArgumentException("No url specified for CustomMultiPart")
        filePartName = cfg.node("file_part_name").string ?: cfg.node("filePartName").string
            ?: throw IllegalArgumentException("No file_part_name specified for CustomMultiPart")
        urlRegex = Regex(cfg.node("url_regex").string ?: cfg.node("urlRegex").string ?: "(.*)")
        
        (cfg.node("extra_params").childrenMap().takeUnless { it.isEmpty() } ?: cfg.node("extraParams").childrenMap()).forEach { (key, value) ->
            this.extraParams[key as String] = value.string ?: ""
        }
    }
    
}