package xyz.xenondevs.nova.util

import xyz.xenondevs.commons.provider.Provider
import kotlin.reflect.KProperty

internal class TimedResettingLong(resetDelay: Provider<Int>) {
    
    private val resetDelay by resetDelay
    private var lastReset = 0
    
    private var value = 0L
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        if ((serverTick - lastReset) / resetDelay > 0) {
            lastReset = serverTick
            value = 0
        }
        
        return value
    }
    
    fun add(value: Long) {
        if ((serverTick - lastReset) / resetDelay > 0) {
            lastReset = serverTick
            this.value = value
        } else {
            this.value += value
        }
    }
    
}