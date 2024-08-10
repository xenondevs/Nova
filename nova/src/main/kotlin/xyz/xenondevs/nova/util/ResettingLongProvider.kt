package xyz.xenondevs.nova.util

import xyz.xenondevs.commons.provider.AbstractProvider
import xyz.xenondevs.commons.provider.Provider

internal class ResettingLongProvider(
    resetDelay: Provider<Int>
) : AbstractProvider<Long>() {
    
    private val resetDelay by resetDelay
    private var lastReset = 0
    
    override fun loadValue(): Long {
        return 0L
    }
    
    override fun get(): Long {
        if ((serverTick - lastReset) / resetDelay > 0) {
            lastReset = serverTick
            set(0)
        }
        
        return super.get()
    }
    
    fun add(value: Long) {
        if ((serverTick - lastReset) / resetDelay > 0) {
            lastReset = serverTick
            set(value)
        } else {
            set(get() + value)
        }
    }
    
}