package xyz.xenondevs.nmsutils.util

internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size) { transform(get(it)) }
}