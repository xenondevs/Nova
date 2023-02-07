package xyz.xenondevs.nova.world.generation.registry

import com.mojang.serialization.Codec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.data.DataFileParser
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.transformer.patch.worldgen.registry.ValueWrapper
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.data.getFirstValueOrThrow

abstract class WorldGenRegistry internal constructor(private val registryAccess: RegistryAccess) {
    
    internal abstract val neededRegistries: Set<ResourceKey<out Registry<*>>>
    
    private val registries = Object2ObjectOpenHashMap<ResourceKey<out Registry<*>>, Registry<*>>()
    private val holderLookup = Object2ObjectOpenHashMap<ResourceKey<out Registry<*>>, HolderGetter<*>>()
    
    init {
        neededRegistries.asSequence().map(registryAccess::registryOrThrow).forEach { registry ->
            NMSUtils.unfreezeRegistry(registry)
            val key = registry.key()
            registries[key] = registry
            holderLookup[key] = (registry as? MappedRegistry<*>)?.createRegistrationLookup() ?: registry.asLookup()
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    protected fun <T: Any> getHolder(id: NamespacedId, registry: ResourceKey<out Registry<T>>): Holder<T> {
        val lookup = holderLookup[registry] as HolderGetter<T>
        val resourceKey = ResourceKey.create(registry, id.resourceLocation)
        return lookup.getOrThrow(resourceKey)
    }
    
    protected fun <T : Any> loadFiles(
        dirName: String,
        codec: Codec<Holder<T>>,
        map: MutableMap<NamespacedId, T>,
        errorName: String = dirName.replace('_', ' ')
    ) {
        DataFileParser.processFiles("worldgen/$dirName") { id, file ->
            val result = codec.decodeJsonFile(file).getFirstValueOrThrow("Failed to parse $errorName of $id at ${file.absolutePath}")
            require(id !in map) { "Duplicate $errorName $id" }
            map[id] = result
        }
    }
    
    @JvmName("loadFiles1")
    protected fun <T : Any> loadFiles(
        dirName: String,
        codec: Codec<T>,
        map: MutableMap<NamespacedId, T>,
        errorName: String = dirName.replace('_', ' ')
    ) {
        DataFileParser.processFiles("worldgen/$dirName") { id, file ->
            val result = codec.decodeJsonFile(file).getFirstOrThrow("Failed to parse $errorName of $id at ${file.absolutePath}")
            require(id !in map) { "Duplicate $errorName $id" }
            map[id] = result
        }
    }
    
    protected fun <T : Any> registerAll(registryKey: ResourceKey<Registry<T>>, map: Map<NamespacedId, T>) {
        val registry = registryAccess.registry(registryKey).get() as Registry<Any>
        map.forEach { (id, value) -> Registry.register(registry, id.toString(":"), ValueWrapper(value)) }
    }
    
    internal abstract fun register()
    
}