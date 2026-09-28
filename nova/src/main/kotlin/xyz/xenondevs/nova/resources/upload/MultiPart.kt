package xyz.xenondevs.nova.resources.upload

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.serialization.kotlinx.RegexSerializer
import java.util.*

@SerialName("custom_multi_part")
@Serializable
internal class CustomMultiPartConfig(
    override val enabled: Boolean,
    private val url: String,
    @SerialName("file_part_name")
    @JsonNames("filePartName")
    private val filePartName: String,
    @SerialName("url_regex")
    @JsonNames("urlRegex")
    @Serializable(with = RegexSerializer::class)
    private val urlRegex: Regex = Regex("(.*)"),
    @SerialName("extra_params")
    @JsonNames("extraParams")
    private val extraParams: Map<String, String> = emptyMap(),
) : UploadServiceConfig {
    
    override fun createService() = MultiPartService(
        url = url,
        filePartName = filePartName,
        urlRegex = urlRegex,
        extraParams = extraParams,
    )
    
}

internal class MultiPartService(
    private val url: String,
    private val filePartName: String,
    private val urlRegex: Regex,
    private val extraParams: Map<String, String>,
) : UploadService {
    
    override suspend fun enable() = Unit
    
    override suspend fun disable() = Unit
    
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
    
}
