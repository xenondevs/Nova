package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD
import xyz.xenondevs.nova.util.register

@DslMarker
internal annotation class RegistryElementBuilderDsl

@RegistryElementBuilderDsl
abstract class RegistryElementBuilder<T : Any>(protected val registry: WritableRegistry<in T>, val id: ResourceLocation) {
    
    protected abstract fun build(): T
    
    internal open fun register(): T {
        if (registry.containsKey(id))
            throw IllegalStateException("Tried to register duplicate element $id in $registry")
        
        val element = build()
        val holder = registry.register(id, element)
        HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(holder, element)
        return element
    }
    
}