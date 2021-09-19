package xyz.xenondevs.nova.util

import me.xdrop.fuzzywuzzy.FuzzySearch

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

fun <E> Collection<E>.searchFor(query: String, getString: (E) -> String): List<E> {
    val elements = HashMap<String, E>()
    
    forEach {
        val string = getString(it)
        if (getString(it).contains(query, true))
            elements[string] = it
    }
    
    return FuzzySearch.extractAll(query, elements.keys)
        .apply { sortByDescending { it.score } }
        .map { elements[it.string]!! }
}
