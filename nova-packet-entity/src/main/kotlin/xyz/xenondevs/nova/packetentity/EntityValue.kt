@file:OptIn(UnstableProviderApi::class)

package xyz.xenondevs.nova.packetentity

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.dsl.DslProperty
import kotlin.reflect.KProperty

internal interface EntityValue<out T> {
    
    fun get(): T
    fun observe(observer: () -> Unit)
    fun unobserve()
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        get()
    
}

internal class DefaultEntityValue<T>(initialValue: T) : DslProperty<T>, EntityValue<T> {
    
    private var provider: Provider<T>? = null
    private var rawValue: T = initialValue
    private var observer: (() -> Unit)? = null
    
    override fun by(provider: Provider<T>) {
        bind(provider)
    }
    
    override fun by(value: T) {
        set(value)
    }
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }
    
    fun bind(provider: Provider<T>) {
        val previous = this.provider
        this.provider = provider
        
        previous?.unobserveWeak(this)
        provider.observeWeak(this) { it.handleProviderUpdate(provider) }
        notifyObserver()
    }
    
    fun set(value: T) {
        rawValue = value
        val previous = provider
        provider = null
        
        previous?.unobserveWeak(this)
        notifyObserver()
    }
    
    override fun get(): T =
        provider?.get() ?: rawValue
    
    override fun observe(observer: () -> Unit) {
        check(this.observer == null) { "DefaultEntityValue already has an observer" }
        this.observer = observer
    }
    
    override fun unobserve() {
        observer = null
    }
    
    private fun notifyObserver() {
        observer?.invoke()
    }
    
    private fun handleProviderUpdate(provider: Provider<T>) {
        if (this.provider === provider)
            notifyObserver()
    }
    
}

internal class FlagsEntityValue(
    private val bits: Array<DefaultEntityValue<Boolean>>
) : EntityValue<Byte> {
    
    private var observer: (() -> Unit)? = null
    
    init {
        for (bit in bits) {
            bit.observe(::notifyObserver)
        }
    }
    
    override fun get(): Byte {
        var flags = 0
        for (i in bits.indices) {
            if (bits[i].get())
                flags = flags or (1 shl i)
        }
        return flags.toByte()
    }
    
    override fun observe(observer: () -> Unit) {
        check(this.observer == null) { "FlagsEntityValue already has an observer" }
        this.observer = observer
    }
    
    override fun unobserve() {
        observer = null
    }
    
    private fun notifyObserver() {
        observer?.invoke()
    }
    
}
