package xyz.xenondevs.nova.world.generation.registry

import com.mojang.serialization.Codec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
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
        errorName: String = dirName.replace('_', ' ')
    ): Map<NamespacedId, T> {
        val map = Object2ObjectOpenHashMap<NamespacedId, T>()
        DataFileParser.processFiles("worldgen/$dirName") { id, file ->
            val result = codec.decodeJsonFile(file).getFirstValueOrThrow("Failed to parse $errorName of $id at ${file.absolutePath}")
            map[id] = result
        }
        return map
    }
    
    @JvmName("loadFiles1")
    protected fun <T : Any> loadFiles(
        dirName: String,
        codec: Codec<T>,
        errorName: String = dirName.replace('_', ' ')
    ): Map<NamespacedId, T> {
        val map = Object2ObjectOpenHashMap<NamespacedId, T>()
        DataFileParser.processFiles("worldgen/$dirName") { id, file ->
            val result = codec.decodeJsonFile(file).getFirstOrThrow("Failed to parse $errorName of $id at ${file.absolutePath}")
            map[id] = result
        }
        return map
    }
    
    internal abstract fun register(registryAccess: RegistryAccess)
    
}