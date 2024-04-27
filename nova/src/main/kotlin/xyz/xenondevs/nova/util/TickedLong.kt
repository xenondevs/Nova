package xyz.xenondevs.nova.util

import kotlin.reflect.KProperty

internal class TickedLong {
    
    private var lastUpdated: Int = 0
    private var value: Long = 0
    private var prevValue: Long = 0
    
    fun get(): Long {
        if (lastUpdated != serverTick) {
            prevValue = value
            value = 0
            lastUpdated = serverTick
        }
        
        return if (value == 0L) prevValue else value
    }
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): Long = get()
    
    fun set(value: Long) {
        if (lastUpdated != serverTick) {
            prevValue = value
            lastUpdated = serverTick
        }
        
        this.value = value
    }
    
    fun add(value: Long) {
        if (lastUpdated != serverTick) {
            prevValue = value
            this.value = 0
            lastUpdated = serverTick
        }
        
        this.value += value
    }
    
}