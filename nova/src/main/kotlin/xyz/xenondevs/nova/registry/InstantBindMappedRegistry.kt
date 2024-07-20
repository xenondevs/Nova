package xyz.xenondevs.nova.registry

import com.mojang.serialization.Lifecycle
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HOLDER_REFERENCE_BIND_VALUE_METHOD

/**
 * [MappedRegistry] implementation that binds the value to the [Holder.Reference] immediately after registering it instead
 * of waiting for [freeze] to be called.
 */
open class InstantBindMappedRegistry<T : Any>(
    resourceKey: ResourceKey<out Registry<T>>,
    lifecycle: Lifecycle
) : MappedRegistry<T>(resourceKey, lifecycle) {
    
    override fun register(key: ResourceKey<T>, value: T, info: RegistrationInfo): Holder.Reference<T> {
        val holder = super.register(key, value, info)
        HOLDER_REFERENCE_BIND_VALUE_METHOD.invoke(holder, value)
        return holder
    }
    
}