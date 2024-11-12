package xyz.xenondevs.nova.resources.upload.service

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.util.StringUtils
import xyz.xenondevs.nova.util.concurrent.Latch
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import xyz.xenondevs.nova.util.startsWithAny
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.exists

@Suppress("HttpUrlsUsage", "ExtractKtorModule")
internal object SelfHost : UploadService {
    
    override val names = listOf("self_host", "selfhost")
    internal val startedLatch = Latch()
    
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
    
    override fun loadConfig(cfg: ConfigurationNode) {
        startedLatch.close()
        val configuredHost = cfg.node("host").string
        this.host = configuredHost ?: ConnectionUtils.SERVER_IP
        
        val port = cfg.node("port").get<Int?>()
        if (port != null) this.port = port
        appendPort = cfg.node("append_port").get<Boolean?>() ?: cfg.node("portNeeded").get<Boolean?>() ?: (configuredHost == null)
        
        thread(name = "ResourcePack Server", isDaemon = true) {
            server = embeddedServer(CIO, port = this.port) {
                routing {
                    get("*") {
                        val packFile = ResourcePackBuilder.RESOURCE_PACK_FILE
                        if (packFile.exists()) call.respondFile(packFile.toFile())
                        else call.respond(HttpStatusCode.NotFound)
                    }
                }
                monitor.subscribe(ServerReady) {
                    thread(isDaemon = true) {
                        startedLatch.open()
                    }
                }
            }
            server.start(wait = true)
        }
    }
    
    override fun disable() {
        server.stop(1000, 1000)
    }
    
    override suspend fun upload(file: Path): String {
        return url + "/" + StringUtils.randomString(5) // https://bugs.mojang.com/browse/MC-251126
    }
    
}