package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.util.set

abstract class RegistryElementBuilder<T : Any>(private val registry: WritableRegistry<in T>, protected val id: ResourceLocation) {
    
    protected abstract fun build(): T
    
    fun register(): T {
        val element = build()
        registry[id] = element
        return element
    }
    
}