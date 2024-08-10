package xyz.xenondevs.nova.registry.vanilla

import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup.RegistryLookup
import net.minecraft.core.HolderSet
import net.minecraft.core.MappedRegistry
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import java.util.*

internal class VanillaLookupWrapper<T>(val registry: MappedRegistry<T>) : RegistryLookup<T> by registry.asLookup() {
    
    private val lookup = registry.createRegistrationLookup()
    
    override fun get(key: ResourceKey<T>): Optional<Holder.Reference<T>> {
        return lookup.get(key)
    }
    
    override fun getOrThrow(key: ResourceKey<T>): Holder.Reference<T> {
        return lookup.getOrThrow(key)
        
    }
    
    override fun get(tag: TagKey<T>): Optional<HolderSet.Named<T>> {
        return lookup.get(tag)
    }
    
    
    override fun getOrThrow(tag: TagKey<T>): HolderSet.Named<T> {
        return lookup.getOrThrow(tag)
    }
    
}