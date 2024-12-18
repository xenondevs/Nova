package xyz.xenondevs.nova.registry

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.Holder
import net.minecraft.core.RegistrationInfo
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.util.data.asDataResult

/**
 * [InstantBindMappedRegistry] implementation that stores an additional [Map] of all entries that maps non-namespaced
 * names to all entries with that name.
 */
class FuzzyMappedRegistry<T : Any>(
    resourceKey: ResourceKey<out Registry<T>>,
    lifecycle: Lifecycle
) : InstantBindMappedRegistry<T>(resourceKey, lifecycle) {
    
    private val byName = Object2ObjectOpenHashMap<String, MutableList<T>>()
    
    override fun byNameCodec(): Codec<T> {
        return Codec.either(super.byNameCodec(), Codec.STRING.flatXmap(
            { name ->
                getByName(name).firstOrNull().asDataResult("No entry with name $name found in registry $this")
            },
            { value ->
                DataResult.success(getKey(value)!!.path)
            }
        )).xmap(
            { it.left().orElseGet { it.right().get() } },
            { Either.left(it) }
        )
    }
    
    override fun register(key: ResourceKey<T>, value: T, info: RegistrationInfo): Holder.Reference<T> {
        val holder = super.register(key, value, info)
        byName.getOrPut(key.location().path, ::ObjectArrayList).add(value)
        return holder
    }
    
    fun getByName(name: String): List<T> = byName[name] ?: emptyList()
    
}