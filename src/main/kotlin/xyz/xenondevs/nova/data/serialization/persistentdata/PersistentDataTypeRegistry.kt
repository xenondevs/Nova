package xyz.xenondevs.nova.data.serialization.persistentdata

import com.google.gson.JsonElement
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.util.data.type
import java.lang.reflect.Type
import java.util.*

val PERSISTENT_DATA_TYPES: Map<Type, PersistentDataType<*, *>> = mapOf(
    type<Byte>() to PersistentDataType.BYTE,
    type<Short>() to PersistentDataType.SHORT,
    type<Int>() to PersistentDataType.INTEGER,
    type<Long>() to PersistentDataType.LONG,
    type<Float>() to PersistentDataType.FLOAT,
    type<Double>() to PersistentDataType.DOUBLE,
    type<String>() to PersistentDataType.STRING,
    type<ByteArray>() to PersistentDataType.BYTE_ARRAY,
    type<IntArray>() to PersistentDataType.INTEGER_ARRAY,
    type<LongArray>() to PersistentDataType.LONG_ARRAY,
    type<PersistentDataContainer>() to PersistentDataType.TAG_CONTAINER,
    type<Array<PersistentDataContainer>>() to PersistentDataType.TAG_CONTAINER_ARRAY,
    type<UUID>() to UUIDDataType,
    type<JsonElement>() to JsonElementDataType,
    type<CompoundElement>() to CompoundElementDataType
)

inline fun <reified K> PersistentDataContainer.get(key: NamespacedKey): K? {
    return get(key, PERSISTENT_DATA_TYPES[type<K>()] as PersistentDataType<*, *>) as K
}

@Suppress("UNCHECKED_CAST")
inline fun <reified K> PersistentDataContainer.set(key: NamespacedKey, data: K) {
    val type = PERSISTENT_DATA_TYPES[type<K>()] as PersistentDataType<*, K>
    set(key, type, data!!)
}