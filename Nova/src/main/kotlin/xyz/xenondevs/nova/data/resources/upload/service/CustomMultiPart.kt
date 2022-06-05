package xyz.xenondevs.nova.data.resources.upload.service

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.data.resources.upload.UploadService
import java.io.File

object CustomMultiPart : UploadService {
    
    override val name = "CustomMultiPart"
    
    private lateinit var url: String
    private lateinit var filePartName: String
    private var urlRegex: Regex? = null
    private val extraParams = HashMap<String, String>()
    
    override fun loadConfig(cfg: ConfigurationSection) {
        url = cfg.getString("url")
            ?: throw IllegalArgumentException("No url specified for CustomMultiPart")
        filePartName = cfg.getString("filePartName")
            ?: throw IllegalArgumentException("No filePartName specified for CustomMultiPart")
        urlRegex = cfg.getString("urlRegex")?.let(::Regex)
        val extraParams = cfg.getConfigurationSection("extraParams")
            ?: return
        extraParams.getValues(false).forEach { (key, value) ->
            this.extraParams[key] = value.toString()
        }
    }
    
    override suspend fun upload(file: File): String {
        val response = HTTP_CLIENT.submitFormWithBinaryData(url, formData {
            append(filePartName, file.readBytes(), Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Zip)
                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
            })
            extraParams.forEach(::append)
        })
        val responseString = response.body<String>()
        require(response.status == HttpStatusCode.OK) { "Upload failed with status ${response.status} amd response $responseString" }
        return if (urlRegex == null) responseString else urlRegex!!.find(responseString)?.groupValues?.getOrNull(1)
            ?: throw IllegalArgumentException("No url found in response: $responseString")
    }
    
    override fun disable() = Unit
    
}