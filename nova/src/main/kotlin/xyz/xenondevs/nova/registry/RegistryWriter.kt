package xyz.xenondevs.nova.registry

import com.mojang.datafixers.util.Either
import net.kyori.adventure.key.Key
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryLoadTask.PendingRegistration
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.toIdentifier
import java.util.stream.Stream

internal sealed interface RegistryWriter<T : Any> {
    
    val key: ResourceKey<out Registry<T>>
    
    operator fun set(key: ResourceKey<T>, value: T)
    
    operator fun set(id: Identifier, value: T) {
        this[ResourceKey.create(key, id)] = value
    }
    
    operator fun set(id: Key, value: T) {
        this[id.toIdentifier()] = value
    }
    
    class Direct<T : Any>(private val registry: WritableRegistry<T>) : RegistryWriter<T> {
        
        override val key: ResourceKey<out Registry<T>> = registry.key()
        
        override fun set(key: ResourceKey<T>, value: T) {
            registry.register(key, value, RegistrationInfo.BUILT_IN)
        }
        
    }
    
    class Deferred<T : Any>(override val key: ResourceKey<out Registry<T>>) : RegistryWriter<T> {
        
        private val entries = ArrayList<PendingRegistration<T>>()
        
        override fun set(key: ResourceKey<T>, value: T) {
            entries += PendingRegistration(key, Either.left(value), RegistrationInfo.BUILT_IN)
        }
        
        fun stream(): Stream<PendingRegistration<T>> = entries.stream()
        
    }
    
}
