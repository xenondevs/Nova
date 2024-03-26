package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.util.set

@DslMarker
internal annotation class RegistryElementBuilderDsl

@RegistryElementBuilderDsl
abstract class RegistryElementBuilder<T : Any>(private val registry: WritableRegistry<in T>, val id: ResourceLocation) {
    
    protected abstract fun build(): T
    
    internal fun register(): T {
        val element = build()
        registry[id] = element
        return element
    }
    
}