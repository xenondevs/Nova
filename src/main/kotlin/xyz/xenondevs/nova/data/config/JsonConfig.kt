package xyz.xenondevs.nova.data.config

import com.google.gson.*
import org.intellij.lang.annotations.RegExp
import xyz.xenondevs.nova.util.data.*
import java.io.File

private fun File.readConfig(): JsonObject {
    check()
    val text = readText(Charsets.UTF_8)
    return if (text.isBlank()) JsonObject() else JsonParser.parseString(text).asJsonObject
}

private fun File.check() {
    if (exists()) return
    val parent = parentFile
    if (parent != null && !parent.exists()) parent.mkdirs()
    createNewFile()
}

open class JsonConfig {
    
    protected val file: File?
    lateinit var config: JsonObject
    
    constructor(file: File, autoInit: Boolean) {
        this.file = file
        if (autoInit) config = file.readConfig()
    }
    
    constructor(config: JsonObject) {
        this.file = null
        this.config = config
    }
    
    fun reload() {
        if (file == null) throw IllegalStateException("This config has no file")
        config = file.readConfig()
    }
    
    fun save(formatted: Boolean = true) {
        if (file == null) throw IllegalStateException("This config has no file")
        val text = (if (formatted) GSON else MINIFIED_GSON).toJson(config)
        file.writeText(text, Charsets.UTF_8)
    }
    
    fun generatePath(path: List<String>): JsonObject {
        val pre = get(path)
        if (pre is JsonObject)
            return pre.asJsonObject
        
        var current = this.config
        path.forEach {
            val pathPart = it.replace("\\.", ".")
            if (current[pathPart] !is JsonObject)
                current[pathPart] = JsonObject()
            current = current[pathPart].asJsonObject
        }
        return current
    }
    
    operator fun set(path: String, element: JsonElement) {
        val pathParts = path.split(PATH_SPLIT_REGEX)
        var parentObject = this.config
        if (pathParts.size > 1)
            parentObject = generatePath(pathParts.dropLast(1))
        parentObject[pathParts.last().replace("\\.", ".")] = element
    }
    
    operator fun set(path: String, value: String) = set(path, JsonPrimitive(value))
    
    operator fun set(path: String, value: Number) = set(path, JsonPrimitive(value))
    
    operator fun set(path: String, value: Boolean) = set(path, JsonPrimitive(value))
    
    operator fun set(path: String, value: Char) = set(path, JsonPrimitive(value))
    
    operator fun set(path: String, value: List<String>) {
        val array = JsonArray()
        value.stream().map { JsonPrimitive(it) }.forEach(array::add)
        set(path, array)
    }
    
    fun addToArray(path: String, vararg elements: JsonElement) {
        var array = getArray(path)
        if (array == null) {
            array = JsonArray()
            set(path, array)
        }
        elements.forEach(array::add)
    }
    
    fun addToArray(path: String, vararg values: String) =
        addToArray(path, *values.map(::JsonPrimitive).toTypedArray())
    
    fun addToArray(path: String, vararg values: Number) =
        addToArray(path, *values.map(::JsonPrimitive).toTypedArray())
    
    fun remove(path: String) {
        val pathParts = path.split(PATH_SPLIT_REGEX)
        if (pathParts.size == 1) {
            this.config.remove(path)
            return
        }
        
        val element = get(pathParts.dropLast(1))
        if (element !is JsonObject)
            return
        val parent = element.asJsonObject
        parent.remove(pathParts.last().replace("\\.", "."))
        if (parent.size() == 0)
            remove(pathParts.dropLast(1).joinToString("."))
    }
    
    fun removeFromArray(path: String, filter: (JsonElement) -> Boolean) {
        val array = getArray(path) ?: return
        val newArray = JsonArray()
        array.filterNot(filter).forEach(newArray::add)
        
        if (newArray.size() == 0) remove(path)
        else set(path, newArray)
    }
    
    fun removeFromArray(path: String, vararg values: String) = removeFromArray(path) { element ->
        element.isString() && values.any { it == element.asString }
    }
    
    fun removeFromArray(path: String, vararg values: Number) = removeFromArray(path) { element ->
        element.isNumber() && values.any { it.toString() == element.asNumber.toString() }
    }
    
    open fun get(path: List<String>): JsonElement? {
        var current = this.config
        path.dropLast(1).forEach { pathPart ->
            val property = pathPart.replace("\\.", ".")
            if (current[property] !is JsonObject)
                return null
            current = current[property].asJsonObject
        }
        
        return current[path.last().replace("\\.", ".")]
    }
    
    operator fun get(path: String) = get(path.split(PATH_SPLIT_REGEX))
    
    @JvmName("get1")
    inline fun <reified T> get(path: String): T? where T : JsonElement {
        val element = get(path)
        return if (element is T) element else null
    }
    
    fun getObject(path: String) = get<JsonObject>(path)
    
    fun getPrimitive(path: String) = get<JsonPrimitive>(path)
    
    fun getArray(path: String) = get<JsonArray>(path)
    
    fun getString(path: String) = getPrimitive(path)?.asString
    
    fun getNumber(path: String) = getPrimitive(path)?.asNumber
    
    fun getInt(path: String) = getNumber(path)?.toInt()
    
    fun getLong(path: String) = getNumber(path)?.toLong()
    
    fun getDouble(path: String) = getNumber(path)?.toDouble()
    
    fun getFloat(path: String) = getNumber(path)?.toFloat()
    
    fun getBoolean(path: String) = getPrimitive(path)?.asBoolean ?: false
    
    fun getChar(path: String) = getPrimitive(path)?.asCharacter
    
    fun getStringList(path: String): ArrayList<String>? {
        val array = getArray(path) ?: return null
        return ArrayList(array.map(JsonElement::getAsString))
    }
    
    fun getOrDefault(path: String, default: String) = getString(path) ?: default
    
    fun getOrThrow(path: String, lazyMessage: () -> String = { "Missing $path in config" }): String {
        require(path in this, lazyMessage)
        return getString(path)!!
    }
    
    
    operator fun contains(path: String) = get(path) != null
    
    @JvmName("contains1")
    inline fun <reified T> contains(path: String) where T : JsonElement = get(path) is T
    
    fun containsString(path: String) = getPrimitive(path).let { it != null && it.isString }
    
    fun containsNumber(path: String) = getPrimitive(path).let { it != null && it.isNumber }
    
    fun containsBoolean(path: String) = getPrimitive(path).let { it != null && it.isBoolean }
    
    operator fun minusAssign(path: String) = remove(path)
    
    companion object {
        private val MINIFIED_GSON = Gson()
        
        @RegExp
        val PATH_SPLIT_REGEX = Regex("""(?<!\\)\Q.\E""")
    }
}

