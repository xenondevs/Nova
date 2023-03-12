package xyz.xenondevs.nova.registry

import com.mojang.serialization.Lifecycle
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

class FuzzyMappedRegistry<T : Any>(
    resourceKey: ResourceKey<out Registry<T>>,
    lifecycle: Lifecycle
) : MappedRegistry<T>(resourceKey, lifecycle) {
    
    private val byName = Object2ObjectOpenHashMap<String, MutableList<T>>()
    
    override fun registerMapping(id: Int, key: ResourceKey<T>, value: T, lifecycle: Lifecycle): Holder.Reference<T> {
        val holder = super.registerMapping(id, key, value, lifecycle)
        byName.getOrPut(key.location().path, ::ObjectArrayList).add(value)
        return holder
    }
    
    fun getByName(name: String): List<T> = byName[name] ?: emptyList()
    
}