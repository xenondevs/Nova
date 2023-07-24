package xyz.xenondevs.nova.data.config

import xyz.xenondevs.commons.provider.Provider

interface Reloadable : Comparable<Reloadable> {
    
    fun reload() = Unit
    
    override fun compareTo(other: Reloadable): Int =
        when {
            other !is ConfigReloadable<*> -> -1
            this is ConfigReloadable<*> -> 0
            else -> 1
        }
    
}

fun <T : Any> configReloadable(initializer: () -> T): Provider<T> = ConfigReloadable(initializer)

@Suppress("DEPRECATION")
private class ConfigReloadable<T : Any>(val initializer: () -> T) : Provider<T>(), ValueReloadable<T> {
    
    init {
        Configs.reloadables += this
    }
    
    override fun loadValue(): T = initializer()
    override fun reload() = update()
    
}