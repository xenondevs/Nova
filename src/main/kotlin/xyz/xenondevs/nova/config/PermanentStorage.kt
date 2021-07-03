package xyz.xenondevs.nova.config

import com.google.gson.JsonObject
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.JSON_PARSER
import xyz.xenondevs.nova.util.fromJson
import java.io.File

object PermanentStorage {
    
    private val file = File("plugins/Nova/storage.json").apply { parentFile.mkdirs() }
    val mainObj: JsonObject = if (file.exists()) JSON_PARSER.parse(file.reader()).asJsonObject else JsonObject()
    
    fun store(key: String, data: Any) {
        mainObj.add(key, GSON.toJsonTree(data))
        file.writeText(GSON.toJson(mainObj))
    }
    
    inline fun <reified T> retrieve(key: String, alternativeProvider: () -> T): T {
        return retrieveOrNull(key) ?: alternativeProvider()
    }
    
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(mainObj.get(key))
    }
    
}