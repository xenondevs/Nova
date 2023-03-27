package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.util.set

abstract class RegistryElementBuilder<T>(val registry: WritableRegistry<in T>, val id: ResourceLocation) {
    
    protected abstract fun build(): T
    
    fun register(): T {
        val element = build()
        registry[id] = element
        return element
    }
    
}