package xyz.xenondevs.nova.util.data

import xyz.xenondevs.commons.collections.getOrSet

@Suppress("UNCHECKED_CAST")
class LazyArray<T : Any>(size: () -> Int, private val init: (Int) -> T) {
    
    private val array: Array<Any?> by lazy { arrayOfNulls(size()) }
    
    operator fun get(index: Int): T = array.getOrSet(index) { init(index) } as T
    
}