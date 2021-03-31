package xyz.xenondevs.nova.util

fun IntRange.toIntArray(): IntArray {
    val array = IntArray(count())
    withIndex().forEach { (index, value) -> array[index] = value }
    return array
}