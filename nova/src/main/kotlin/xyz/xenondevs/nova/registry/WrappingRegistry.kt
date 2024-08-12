package xyz.xenondevs.nova.registry

import com.mojang.serialization.Lifecycle
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceKey

class WrappingRegistry<T : Any, W : Any>(
    key: ResourceKey<out Registry<T>>,
    lifecycle: Lifecycle,
    private val wrapperRegistry: WritableRegistry<W>,
    private val toWrapper: (T) -> (W)
) : MappedRegistry<T>(key, lifecycle) {
    
    override fun register(key: ResourceKey<T>, value: T, info: RegistrationInfo): Holder.Reference<T> {
        wrapperRegistry.register(ResourceKey.create(wrapperRegistry.key(), key.location()), toWrapper(value), info)
        return super.register(key, value, info)
    }
    
}