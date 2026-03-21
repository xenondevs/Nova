package xyz.xenondevs.nova.resources.upload

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import xyz.xenondevs.nova.util.startsWithAny
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

private val UUID_REGEX = Regex("""^(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$""", RegexOption.IGNORE_CASE)
private val PACKS_FOLDER = DATA_FOLDER.resolve("resource_pack/.self_host/")

@SerialName("self_host")
@Serializable
internal class SelfHostConfig(
    override val enabled: Boolean,
    private val host: String? = null,
    private val port: Int = 38519,
    @JsonNames("portNeeded")
    @SerialName("append_port")
    private val appendPort: Boolean = host != null,
) : UploadServiceConfig {
    
    override fun createService() = SelfHostService(
        host = host ?: ConnectionUtils.SERVER_IP,
        port = port,
        appendPort = appendPort,
    )
    
}

internal class SelfHostService(
    private val host: String,
    private val port: Int,
    private val appendPort: Boolean,
) : UploadService {
    
    private val url: String
        get() = buildString {
            if (!host.startsWithAny("http://", "https://"))
                append("http://")
            append(host)
            if (appendPort)
                append(":${port}")
        }
    
    private lateinit var server: EmbeddedServer<CIOApplicationEngine, *>
    
    override suspend fun enable() {
        server = embeddedServer(CIO, port = this.port) {
            routing {
                get(UUID_REGEX) {
                    val file = resolvePack(UUID.fromString(call.parameters["uuid"]!!))
                    if (file.exists()) {
                        call.respondOutputStream(
                            ContentType.Application.Zip,
                            HttpStatusCode.OK,
                            file.fileSize(),
                        ) { file.inputStream().use { it.transferTo(this) } }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
        server.startSuspend()
    }
    
    override suspend fun disable() {
        server.stopSuspend(1000, 1000)
    }
    
    override suspend fun upload(id: UUID, bin: ByteArray): String = withContext(Dispatchers.IO) {
        val file = resolvePack(id)
        file.createParentDirectories()
        file.writeBytes(bin)
        return@withContext "$url/$id"
    }
    
    private fun resolvePack(id: UUID): Path =
        PACKS_FOLDER.resolve("$id.zip")
    
}
