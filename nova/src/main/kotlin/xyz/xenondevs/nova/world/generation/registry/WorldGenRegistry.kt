package xyz.xenondevs.nova.world.generation.registry

import com.mojang.serialization.Codec
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import xyz.xenondevs.nova.data.DataFileParser
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.data.getFirstValueOrThrow

abstract class WorldGenRegistry internal constructor() {
    
    internal abstract val neededRegistries: Set<ResourceKey<out Registry<*>>>
    
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
    
    protected fun <T: Any> registerAll(registryAccess: RegistryAccess, registryKey: ResourceKey<Registry<T>>, map: Map<NamespacedId, T>) {
        val registry = registryAccess.registry(registryKey).get()
        map.forEach { (id, value) -> Registry.register(registry, id.toString(":"), value) }
    }
    
    internal abstract fun register(registryAccess: RegistryAccess)
    
}