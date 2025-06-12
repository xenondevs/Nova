package xyz.xenondevs.nova.serialization.persistentdata

import net.kyori.adventure.key.Key
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import xyz.xenondevs.nova.util.toNamespacedKey

/**
 * Writes [obj] under [key] as [T] using CBF. If [obj] is null, removes [key] from the container.
 */
inline operator fun <reified T : Any> PersistentDataContainer.set(key: Key, obj: T?) {
    if (obj != null) {
        set(key.toNamespacedKey(), PersistentDataType.BYTE_ARRAY, Cbf.write(obj))
    } else {
        remove(key.toNamespacedKey())
    }
}

/**
 * Writes [obj] under [key] as [T] using [serializer]. If [obj] is null, removes [key] from the container.
 */
fun <T : Any> PersistentDataContainer.set(key: Key, serializer: BinarySerializer<T>, obj: T?) {
    if (obj != null) {
        set(key.toNamespacedKey(), PersistentDataType.BYTE_ARRAY, serializer.write(obj))
    } else {
        remove(key.toNamespacedKey())
    }
}

/**
 * Reads the value under [key] as [T] using CBF, or null if there is no value under [key].
 */
inline operator fun <reified T> PersistentDataContainer.get(key: Key): T? =
    get(key.toNamespacedKey(), PersistentDataType.BYTE_ARRAY)?.let(Cbf::read)

/**
 * Reads the value under [key] as [T] using [serializer], or null if there is no value under [key].
 */
fun <T : Any> PersistentDataContainer.get(key: Key, serializer: BinarySerializer<T>): T? =
    get(key.toNamespacedKey(), PersistentDataType.BYTE_ARRAY)?.let(serializer::read)