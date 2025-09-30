package xyz.xenondevs.nova.resources.upload.service

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.util.data.get
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

@Suppress("HttpUrlsUsage", "ExtractKtorModule")
internal object SelfHost : UploadService {
    
    override val names = setOf("self_host", "selfhost")
    
    private lateinit var server: EmbeddedServer<CIOApplicationEngine, *>
    
    private lateinit var host: String
    private var port = 38519
    private var appendPort = true
    
    private val url: String
        get() = buildString {
            if (!host.startsWithAny("http://", "https://"))
                append("http://")
            append(host)
            if (appendPort)
                append(":$port")
        }
    
    override suspend fun enable(cfg: ConfigurationNode) {
        val configuredHost = cfg.node("host").string
        this.host = configuredHost ?: ConnectionUtils.SERVER_IP
        
        val port = cfg.node("port").get<Int?>()
        if (port != null) 
            this.port = port
        
        appendPort = cfg.node("append_port").get<Boolean?>()
            ?: cfg.node("portNeeded").get<Boolean?>()
                ?: (configuredHost == null)
        
        startServer()
    }
    
    private suspend fun startServer() {
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