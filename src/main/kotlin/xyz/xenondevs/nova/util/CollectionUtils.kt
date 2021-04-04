package xyz.xenondevs.nova.util

fun <E> List<E>.contentEquals(other: List<E>) = containsAll(other) && other.containsAll(this)

fun <E> Set<E>.contentEquals(other: Set<E>) = containsAll(other) && other.containsAll(this)