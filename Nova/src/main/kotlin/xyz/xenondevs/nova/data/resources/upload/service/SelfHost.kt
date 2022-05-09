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
import xyz.xenondevs.nova.util.data.getBooleanOrNull
import xyz.xenondevs.nova.util.data.getIntOrNull
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import xyz.xenondevs.nova.util.startsWithAny
import java.io.File
import kotlin.concurrent.thread

object SelfHost : UploadService {
    
    private val selfHostDir = File(NOVA.dataFolder, "ResourcePack/autohost")
    private val packFile = File(selfHostDir, "pack.zip")
    
    override val name = "SelfHost"
    
    lateinit var host: String
    var port = 38519
    var portNeeded = true
    
    val url: String
        get() = buildString {
            if (!host.startsWithAny("http://", "https://"))
                append("http://")
            append(host)
            if (portNeeded)
                append(":$port")
        }
    
    override fun loadConfig(cfg: ConfigurationSection) {
        val configuredHost = cfg.getString("host")
        this.host = configuredHost ?: ConnectionUtils.SERVER_IP
        
        val port = cfg.getIntOrNull("port")
        if (port != null) this.port = port
        portNeeded = cfg.getBooleanOrNull("portNeeded") ?: (configuredHost != null)
        
        var server: NettyApplicationEngine? = null
        
        thread(name = "ResourcePack Server", isDaemon = true) {
            server = embeddedServer(Netty, port = this.port) {
                routing {
                    get {
                        if (packFile.exists()) call.respondFile(packFile)
                        else call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
            server!!.start(wait = true)
        }
        NOVA.disableHandlers += { server!!.stop(1000, 1000) }
    }
    
    override suspend fun upload(file: File): String {
        file.copyTo(packFile, overwrite = true)
        return url
    }
    
}