package xyz.xenondevs.nova.util.data

import xyz.xenondevs.nova.util.getOrSet

@Suppress("UNCHECKED_CAST")
class LazyArray<T : Any>(size: Int, private val init: (Int) -> T) {
    
    private val array: Array<Any?> = arrayOfNulls(size)
    
    operator fun get(index: Int): T = array.getOrSet(index) { init(index) } as T
    
}