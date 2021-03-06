package xyz.xenondevs.nova.data.resources.upload.service

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.util.StringUtils
import xyz.xenondevs.nova.util.concurrent.Latch
import xyz.xenondevs.nova.util.data.getBooleanOrNull
import xyz.xenondevs.nova.util.data.getIntOrNull
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import xyz.xenondevs.nova.util.startsWithAny
import java.io.File
import kotlin.concurrent.thread

@Suppress("HttpUrlsUsage")
internal object SelfHost : UploadService {
    
    private val selfHostDir = File(NOVA.dataFolder, "resource_pack/self_host")
    private val packFile = File(selfHostDir, "pack.zip")
    private lateinit var server: NettyApplicationEngine
    internal val startedLatch = Latch()
    
    override val name = "SelfHost"
    
    private lateinit var host: String
    private var port = 38519
    private var portNeeded = true
    
    private val url: String
        get() = buildString {
            if (!host.startsWithAny("http://", "https://"))
                append("http://")
            append(host)
            if (portNeeded)
                append(":$port")
        }
    
    override fun loadConfig(cfg: ConfigurationSection) {
        startedLatch.on()
        val configuredHost = cfg.getString("host")
        this.host = configuredHost ?: ConnectionUtils.SERVER_IP
        
        val port = cfg.getIntOrNull("port")
        if (port != null) this.port = port
        portNeeded = cfg.getBooleanOrNull("portNeeded") ?: (configuredHost != null)
        
        thread(name = "ResourcePack Server", isDaemon = true) {
            server = embeddedServer(Netty, port = this.port) {
                routing {
                    get("*") {
                        if (packFile.exists()) call.respondFile(packFile)
                        else call.respond(HttpStatusCode.NotFound)
                    }
                }
                environment.monitor.subscribe(ApplicationStarted) {
                    thread(isDaemon = true) {
                        Thread.sleep(1000) // https://youtrack.jetbrains.com/issue/KTOR-4259
                        startedLatch.off()
                    }
                }
            }
            server.start(wait = true)
        }
    }
    
    override fun disable() {
        server.stop(1000, 1000)
    }
    
    override suspend fun upload(file: File): String {
        file.copyTo(packFile, overwrite = true)
        return url + "/" + StringUtils.randomString(5) // https://bugs.mojang.com/browse/MC-251126
    }
    
}