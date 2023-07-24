package xyz.xenondevs.nova.util.data

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.collections.contentEquals
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import java.io.StringReader
import java.util.*

fun YamlConfiguration.copy(): YamlConfiguration {
    return YamlConfiguration.loadConfiguration(StringReader(saveToString()))
}

fun ConfigurationSection.saveToString(includeComments: Boolean): String {
    val cfg = YamlConfiguration()
    
    if (includeComments) {
        getKeys(true).forEach {
            cfg.set(it, get(it))
            cfg.setComments(it, getComments(it))
            cfg.setInlineComments(it, getInlineComments(it))
        }
    } else getKeys(false).forEach { cfg.set(it, get(it)) }
    
    return cfg.saveToString()
}

fun ConfigurationSection.hash(): Int {
    return saveToString(false).hashCode()
}

fun ConfigurationSection.contentEquals(other: ConfigurationSection): Boolean {
    val keys = getKeys(true)
    val otherKeys = other.getKeys(true)
    return keys.size == otherKeys.size && keys.all { path ->
        val a = get(path)
        val b = other.get(path)
        
        if (a is ConfigurationSection && b is ConfigurationSection)
            return@all true
        
        if (a is List<*> && b is List<*>)
            return@all a.contentEquals(b)
        
        return@all a == b
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> ConfigurationSection.getListOrNull(path: String): List<T>? =
    if (isSet(path)) getList(path) as List<T> else null

@Suppress("UNCHECKED_CAST")
fun ConfigurationSection.getConfigurationSectionListOrNull(path: String): List<ConfigurationSection>? =
    if (isSet(path)) getMapList(path).map { ConfigurationSection(this, path, it as Map<String, Any>) } else null

fun ConfigurationSection.getConfigurationSectionList(path: String): List<ConfigurationSection> =
    getConfigurationSectionListOrNull(path) ?: emptyList()

fun ConfigurationSection.getBooleanListOrNull(path: String): List<Boolean>? =
    if (isSet(path)) getBooleanList(path) else null

fun ConfigurationSection.getCharListOrNull(path: String): List<Char>? =
    if (isSet(path)) getCharacterList(path) else null

fun ConfigurationSection.getByteListOrNull(path: String): List<Byte>? =
    if (isSet(path)) getByteList(path) else null

fun ConfigurationSection.getShortListOrNull(path: String): List<Short>? =
    if (isSet(path)) getShortList(path) else null

fun ConfigurationSection.getIntListOrNull(path: String): List<Int>? =
    if (isSet(path)) getIntegerList(path) else null

fun ConfigurationSection.getLongListOrNull(path: String): List<Long>? =
    if (isSet(path)) getLongList(path) else null

fun ConfigurationSection.getFloatListOrNull(path: String): List<Float>? =
    if (isSet(path)) getFloatList(path) else null

fun ConfigurationSection.getDoubleListOrNull(path: String): List<Double>? =
    if (isSet(path)) getDoubleList(path) else null

fun ConfigurationSection.getStringListOrNull(path: String): List<String>? =
    if (isSet(path)) getStringList(path) else null

fun ConfigurationSection.getBooleanOrNull(path: String): Boolean? =
    if (isSet(path)) getBoolean(path) else null

fun ConfigurationSection.getByteOrNull(path: String): Byte? =
    if (isSet(path)) getInt(path).toByte() else null

fun ConfigurationSection.getShortOrNull(path: String): Short? =
    if (isSet(path)) getInt(path).toShort() else null

fun ConfigurationSection.getIntOrNull(path: String): Int? =
    if (isSet(path)) getInt(path) else null

fun ConfigurationSection.getLongOrNull(path: String): Long? =
    if (isSet(path)) getLong(path) else null

fun ConfigurationSection.getDoubleOrNull(path: String): Double? =
    if (isSet(path)) getDouble(path) else null

fun ConfigurationSection.getFloatOrNull(path: String): Float? =
    if (isSet(path)) getDouble(path).toFloat() else null

fun ConfigurationSection.getByte(path: String, def: Byte = 0): Byte =
    getByteOrNull(path) ?: def

fun ConfigurationSection.getShort(path: String, def: Short = 0): Short =
    getShortOrNull(path) ?: def

fun ConfigurationSection.getFloat(path: String, def: Float = 0f): Float =
    getFloatOrNull(path) ?: def

@Suppress("UNCHECKED_CAST")
fun ConfigurationSection.toMap(): Map<String, Any> {
    val sectionPathMap = ReflectionRegistry.MEMORY_SECTION_MAP_FIELD.get(this) as MutableMap<String, Any>
    return sectionPathMap.mapValues { ReflectionRegistry.SECTION_PATH_DATA_DATA_FIELD.get(it) }
}

fun ConfigurationSection(parent: ConfigurationSection, path: String, map: Map<String, Any>): ConfigurationSection {
    val memorySection = ReflectionRegistry.MEMORY_SECTION_CONSTRUCTOR.newInstance(parent, path)
    val sectionPathDataMap = map.mapValues { ReflectionRegistry.SECTION_PATH_DATA_CONSTRUCTOR.newInstance(it.value) }
    ReflectionRegistry.MEMORY_SECTION_MAP_FIELD.set(memorySection, sectionPathDataMap)
    return memorySection
}

@Suppress("UNCHECKED_CAST")
fun <T : ConfigurationNode> T.walk(): Sequence<T> = sequence {
    val unexplored = LinkedList<T>()
    unexplored += this@walk
    while (unexplored.isNotEmpty()) {
        val node = unexplored.poll()
        for ((_, child) in node.childrenMap()) {
            unexplored += child as T
        }
        yield(node)
    }
}