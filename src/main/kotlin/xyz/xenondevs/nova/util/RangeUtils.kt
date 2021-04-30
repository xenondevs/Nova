package xyz.xenondevs.nova.util

fun IntRange.toIntArray(): IntArray {
    val array = IntArray(count())
    withIndex().forEach { (index, value) -> array[index] = value }
    return array
}

val IntRange.size: Int
    get() = last - first + 1
