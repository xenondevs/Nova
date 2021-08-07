package xyz.xenondevs.nova.util

import java.util.*

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>) = EnumMap<K, V>(K::class.java).apply { putAll(pairs) }

inline fun <reified K : Enum<K>, V> emptyEnumMap() = EnumMap<K, V>(K::class.java)

inline fun <reified K : Enum<K>, V> Map<K, V>.toEnumMap() = this.toMap(EnumMap(K::class.java))

@Suppress("UNCHECKED_CAST")
inline fun <reified R, K, V> Map<K, V>.filterIsInstanceValues() = filter { it.value is R } as Map<K, R>

@Suppress("UNCHECKED_CAST")
inline fun <reified R, K, V> Map<K, V>.filterIsInstanceKeys() = filter { it.key is R } as Map<R, V>

inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean): MutableMap<K, V> {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (predicate(entry)) iterator.remove()
    }
    
    return this
}
