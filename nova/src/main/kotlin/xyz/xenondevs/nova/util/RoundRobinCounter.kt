package xyz.xenondevs.nova.util

internal class RoundRobinCounter(val maxExclusive: Int) {
    
    private var i = 0
    
    fun get() = i
    
    fun increment() {
        i++
        if (i == maxExclusive) i = 0
    }
    
}