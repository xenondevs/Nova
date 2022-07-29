package xyz.xenondevs.nova.data.serialization.persistentdata

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.cbf.CBF

fun PersistentDataContainer.set(key: NamespacedKey, obj: Any?) =
    set(key, PersistentDataType.BYTE_ARRAY, CBF.write(obj))

inline fun <reified T : Any> PersistentDataContainer.get(key: NamespacedKey): T? =
    get(key, PersistentDataType.BYTE_ARRAY)?.let(CBF::read)