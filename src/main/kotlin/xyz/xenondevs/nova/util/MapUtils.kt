package xyz.xenondevs.nova.util

import java.util.*

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>) = EnumMap<K, V>(K::class.java).apply { putAll(pairs) }

inline fun <reified K : Enum<K>, V> emptyEnumMap() = EnumMap<K, V>(K::class.java)

inline fun <reified R, K, V> Map<K, V>.filterIsInstanceValues() = filter { it.value is R } as Map<K, R>

inline fun <reified R, K, V> Map<K, V>.filterIsInstanceKeys() = filter { it.key is R } as Map<R, V>
