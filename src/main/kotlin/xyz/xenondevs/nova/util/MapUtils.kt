package xyz.xenondevs.nova.util

import java.util.*

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>) = EnumMap<K, V>(K::class.java).apply { putAll(pairs) }
