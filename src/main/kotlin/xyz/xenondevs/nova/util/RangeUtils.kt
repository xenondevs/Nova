package xyz.xenondevs.nova.util

fun IntRange.toIntArray(): IntArray {
    var current = this.first - 1
    return IntArray(size) { ++current }
}

val IntRange.size: Int
    get() = last - first + 1
