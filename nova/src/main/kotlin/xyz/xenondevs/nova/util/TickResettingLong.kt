package xyz.xenondevs.nova.util

import kotlin.reflect.KProperty

internal class TickResettingLong {
    
    private var accumulatorTick = 0
    
    private var value = 0L
    private var accumulator = 0L
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        checkCompleteAccumulation()
        return value
    }
    
    fun add(value: Long) {
        checkCompleteAccumulation()
        accumulator += value
    }
    
    private fun checkCompleteAccumulation() {
        if (serverTick > accumulatorTick) {
            if (serverTick == (accumulatorTick + 1)) {
                value = accumulator
                accumulator = 0
            } else {
                value = 0
                accumulator = 0
            }
            
            accumulatorTick = serverTick 
        }
    }
    
}