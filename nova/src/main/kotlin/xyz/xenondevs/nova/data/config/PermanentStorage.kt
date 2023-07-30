package xyz.xenondevs.nova.data.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.data.serialization.json.GSON
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

internal object PermanentStorage {
    
    private val dir = File( "plugins/Nova/.internal_data/storage/")
    private val storage = HashMap<String, JsonElement>()
    
    init {
        // legacy conversion
        val legacyFile = File("plugins/Nova/storage.do-not-edit")
        if (legacyFile.exists()) {
            val legacyObj = legacyFile.parseJson() as JsonObject
            for ((key, element) in legacyObj.entrySet()) {
                val f = File(dir, "$key.json")
                f.parentFile.mkdirs()
                element.writeToFile(f)
            }
            legacyFile.delete()
        }
        
        // load storage map
        dir.walk()
            .filter(File::isFile)
            .forEach {
                val key = it.relativeTo(dir).invariantSeparatorsPath.removeSuffix(".json")
                storage[key] = it.parseJson()
            }
    }
    
    fun store(key: String, data: Any?) {
        val f = getFile(key)
        if (data != null) {
            val json = GSON.toJsonTree(data)
            storage[key] = json
            f.parentFile.mkdirs()
            json.writeToFile(f)
        } else {
            f.delete()
        }
    }
    
    fun has(key: String): Boolean {
        return key in storage
    }
    
    fun remove(key: String) {
        store(key, null)
    }
    
    inline fun <reified T> retrieve(key: String, alternativeProvider: () -> T): T {
        return retrieveOrNull(key) ?: alternativeProvider()
    }
    
    fun <T> retrieve(type: Type, key: String, alternativeProvider: () -> T): T {
        return retrieveOrNull<T>(type, key) ?: alternativeProvider()
    }
    
    fun <T> retrieve(type: KType, key: String, alternativeProvider: () -> T): T {
        return retrieve(type.javaType, key, alternativeProvider)
    }
    
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(storage[key])
    }
    
    fun <T> retrieveOrNull(type: Type, key: String): T? {
        return GSON.fromJson(storage[key], type) as? T
    }
    
    fun <T> retrieveOrNull(type: KType, key: String): T? {
        return retrieveOrNull(type.javaType, key)
    }
    
    private fun getFile(key: String) = File(dir, "$key.json")
    
}