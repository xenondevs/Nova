package xyz.xenondevs.nova.data.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.io.File

internal object PermanentStorage {
    
    private val file: File = File("${NOVA.dataFolder}/storage.do-not-edit").also { it.parentFile.mkdirs() }
    val mainObj: JsonObject
    
    init {
        val legacyFile = File("${NOVA.dataFolder}/storage.json")
        if (!file.exists() && legacyFile.exists())
            legacyFile.renameTo(file)
        
        mainObj = if (file.exists())
            JsonParser.parseReader(file.reader()).asJsonObject
        else JsonObject()
    }
    
    fun store(key: String, data: Any?) {
        if (data != null)
            mainObj.add(key, GSON.toJsonTree(data))
        else mainObj.remove(key)
        
        file.writeText(GSON.toJson(mainObj))
    }
    
    fun has(key: String): Boolean {
        return mainObj.has(key)
    }
    
    fun remove(key: String) {
        mainObj.remove(key)
        file.writeText(GSON.toJson(mainObj))
    }
    
    inline fun <reified T> retrieve(key: String, alternativeProvider: () -> T): T {
        return retrieveOrNull(key) ?: alternativeProvider()
    }
    
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(mainObj.get(key))
    }
    
}