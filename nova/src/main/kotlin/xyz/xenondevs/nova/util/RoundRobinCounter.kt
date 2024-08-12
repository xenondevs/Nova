package xyz.xenondevs.nova.util

internal class RoundRobinCounter(private val maxExclusive: Int) {
    
    private var i = 0
    
    fun next(): Int {
        i = (i + 1) % maxExclusive
        return i
    }
    
}