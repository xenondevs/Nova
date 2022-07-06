package xyz.xenondevs.nmsutils.util

internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size) { transform(get(it)) }
}

inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean): MutableMap<K, V> {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (predicate(entry)) iterator.remove()
    }
    
    return this
}