package xyz.xenondevs.nova.util.data.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import xyz.xenondevs.nova.HTTP_CLIENT
import java.net.URL

object ConnectionUtils {
    
    val SERVER_IP by lazy { runBlocking { HTTP_CLIENT.get("https://checkip.amazonaws.com/").body<String>().trim() } }
    
    fun isURL(url: String) =
        runCatching { URL(url) }.isSuccess
    
}