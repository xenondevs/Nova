package xyz.xenondevs.nova.util

import me.xdrop.fuzzywuzzy.FuzzySearch

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