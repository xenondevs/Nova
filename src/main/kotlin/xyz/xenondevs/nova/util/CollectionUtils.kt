package xyz.xenondevs.nova.util

fun <E> List<E>.contentEquals(other: List<E>) = size == other.size && containsAll(other)

fun <E> Set<E>.contentEquals(other: Set<E>) = size == other.size && containsAll(other)

fun <E> MutableList<E>.removeFirstWhere(test: (E) -> Boolean): Boolean {
    val iterator = iterator()
    while (iterator.hasNext()) {
        if (test(iterator.next())) {
            iterator.remove()
            return true
        }
    }
    
    return false
}
