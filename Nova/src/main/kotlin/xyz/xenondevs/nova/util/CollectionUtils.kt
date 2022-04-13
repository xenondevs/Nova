package xyz.xenondevs.nova.util

import de.studiocode.invui.item.Item
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.checkerframework.checker.units.qual.K
import java.util.*

fun <E> List<E>.contentEquals(other: List<E>) = size == other.size && containsAll(other)

fun <E> Set<E>.contentEquals(other: Set<E>) = size == other.size && containsAll(other)

fun <E> MutableIterable<E>.removeFirstWhere(test: (E) -> Boolean): Boolean {
    val iterator = iterator()
    while (iterator.hasNext()) {
        if (test(iterator.next())) {
            iterator.remove()
            return true
        }
    }
    
    return false
}

fun <E> MutableIterable<E>.pollFirstWhere(test: (E) -> Boolean): E? {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val element = iterator.next()
        if (test(element)) {
            iterator.remove()
            return element
        }
    }
    
    return null
}

fun <E> MutableIterable<E>.pollFirst(): E? {
    val iterator = iterator()
    return if (iterator.hasNext()) {
        val element = iterator.next()
        iterator.remove()
        element
    } else null
}

inline fun <K, V> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    val destination = LinkedHashMap<K, V>()
    return associateWithNotNullTo(destination, valueSelector)
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

@Suppress("NOTHING_TO_INLINE")
inline fun List<Item>.notifyWindows(): Unit = forEach(Item::notifyWindows)

fun <T> Array<T?>.getOrSet(index: Int, lazyValue: () -> T): T {
    var value = get(index)
    if (value == null) {
        value = lazyValue()
        set(index, value)
    }
    
    return value!!
}

/**
 * Puts the [value] in the map if it is not null.
 * Removes the [key] from the map if the [value] is null.
 */
fun <K, V> MutableMap<K, V>.putOrRemove(key: K, value: V?) {
    if (value == null) remove(key)
    else put(key, value)
}

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>) = EnumMap<K, V>(K::class.java).apply { putAll(pairs) }

inline fun <reified K : Enum<K>, V> emptyEnumMap() = EnumMap<K, V>(K::class.java)

inline fun <reified K : Enum<K>, V> Map<K, V>.toEnumMap() = this.toMap(EnumMap(K::class.java))

inline fun <reified K : Enum<K>, V> Iterable<K>.associateWithToEnumMap(valueSelector: (K) -> V): EnumMap<K, V> {
    val destination = EnumMap<K, V>(K::class.java)
    for (element in this) destination[element] = valueSelector(element)
    return destination
}

@Suppress("UNCHECKED_CAST")
inline fun <reified R, K, V> Map<K, V>.filterIsInstanceValues() = filter { it.value is R } as Map<K, R>

@Suppress("UNCHECKED_CAST")
inline fun <reified R, K, V> Map<K, V>.filterIsInstanceKeys() = filter { it.key is R } as Map<R, V>

inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean): MutableMap<K, V> {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        if (predicate(entry)) iterator.remove()
    }
    
    return this
}

inline fun <K, V, M : MutableMap<K, V>> Iterable<K>.associateWithNotNullTo(destination: M, valueSelector: (K) -> V?): M {
    for (element in this) {
        val value = valueSelector(element)
        if (value != null) destination[element] = value
    }
    
    return destination
}

fun <K, V> Map<K, V>.getValues(keys: Iterable<K>): List<V> {
    val values = ArrayList<V>()
    for (key in keys) {
        values += get(key)!!
    }
    return values
}

fun <T> MutableList<T>.rotateRight() {
    val last = removeAt(size - 1)
    add(0, last)
}

fun <T> MutableList<T>.rotateLeft() {
    val first = removeAt(0)
    add(first)
}

fun <K, V> LinkedHashMap<K, V>.poll(): Map.Entry<K, V>? {
    return entries.pollFirst()
}

inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size) { transform(get(it)) }
}

inline fun <T> List<T>.mapToIntArray(transform: (T) -> Int): IntArray {
    return IntArray(size) { transform(get(it)) }
}