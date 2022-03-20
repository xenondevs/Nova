package xyz.xenondevs.nova.data.resources.upload.service

import de.studiocode.invui.resourcepack.ForceResourcePack
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.JsonConfig
import xyz.xenondevs.nova.data.resources.upload.UploadService
import java.io.File
import kotlin.concurrent.thread

object SelfHost : UploadService {
    
    private val selfHostDir = File(NOVA.dataFolder, "ResourcePack/autohost")
    private val packFile = File(selfHostDir, "pack.zip")
    
    override val name = "SelfHost"
    var host = "localhost" // TODO retrieve ip as default host
    var port = 38519
    val url: String
        get() = "http://$host:$port"
    
    override fun loadConfig(json: JsonConfig) {
        val host = json.getString("host")
        if (host != null) this.host = host
        
        val port = json.getInt("port")
        if (port != null) this.port = port
        
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
    
    override fun upload(file: File): String {
        file.copyTo(packFile, overwrite = true)
        return url
    }
    
}