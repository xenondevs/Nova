package xyz.xenondevs.nova.data.resources.upload.service

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import xyz.xenondevs.nova.data.config.JsonConfig
import xyz.xenondevs.nova.data.resources.upload.UploadService
import java.io.File
import kotlin.concurrent.thread

object SelfHost : UploadService {
    
    private const val DOWNLOAD_URL_FORMAT = "http://%s:%d/"
    
    override val name = "SelfHost"
    var host = "localhost" // TODO retrieve ip as default host
    var port = 38519
    private var file: File? = null
    
    override fun loadConfig(json: JsonConfig) {
        val host = json.getString("host")
        if (host != null) this.host = host
        
        val port = json.getInt("port")
        if (port != null) this.port = port
        
        thread(name = "ResourcePack Server", isDaemon = true) {
            embeddedServer(Netty, port = this.port) {
                routing {
                    get {
                        if (file == null) call.respond(HttpStatusCode.NotFound)
                        else call.respondFile(file!!)
                    }
                }
            }.start(wait = true)
        }
    }
    
    override fun upload(file: File): String {
        this.file = file
        return DOWNLOAD_URL_FORMAT.format(host, port)
    }
    
}