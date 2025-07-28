package xyz.xenondevs.nova.resources.builder.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition
import xyz.xenondevs.nova.util.data.readJson
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * A [PackBuildData] that deals with block state definition files.
 */
class BlockStateContent(private val builder: ResourcePackBuilder) : PackBuildData {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val vanillaDefinitions = HashMap<ResourcePath<ResourceType.BlockStateDefinition>, BlockStateDefinition?>()
    private val customDefinitions = HashMap<ResourcePath<ResourceType.BlockStateDefinition>, BlockStateDefinition?>()
    
    /**
     * Retrieves the block state definition with the specified [path] or null if it does not exist.
     */
    operator fun get(path: ResourcePath<ResourceType.BlockStateDefinition>): BlockStateDefinition? =
        getCustom(path) ?: getVanilla(path)
    
    /**
     * Retrieves the vanilla block state definition with the specified [path] or null if it does not exist.
     * 
     * Vanilla block state definitions are shipped with the game and are not part of the resource pack.
     */
    fun getVanilla(path: ResourcePath<ResourceType.BlockStateDefinition>): BlockStateDefinition? =
        vanillaDefinitions.getOrPut(path) { loadDefinition(builder.resolveVanilla(path)) }
    
    /**
     * Retrieves the custom block state definition with the specified [path] or null if it does not exist.
     */
    fun getCustom(path: ResourcePath<ResourceType.BlockStateDefinition>): BlockStateDefinition? =
        customDefinitions.getOrPut(path) { loadDefinition(builder.resolve(path)) }
    
    /**
     * Sets the custom [definition] under the given [path], replacing any existing definition under that [path].
     */
    operator fun set(path: ResourcePath<ResourceType.BlockStateDefinition>, definition: BlockStateDefinition) {
        customDefinitions[path] = definition
    }
    
    private fun loadDefinition(path: Path): BlockStateDefinition? {
        if (path.notExists())
            return null
        
        try {
            return path.readJson<BlockStateDefinition>(json)
        } catch (e: Exception) {
            builder.logger.warn("Failed to parse block state definition at $path", e)
        }
        
        return null
    }
    
    /**
     * Loads all block state definitions, both vanilla and custom.
     */
    inner class Load : PackTask {
        
        override suspend fun run() = coroutineScope {
            val vanilla = ArrayList<Deferred<Pair<ResourcePath<ResourceType.BlockStateDefinition>, BlockStateDefinition?>>>()
            val custom = ArrayList<Deferred<Pair<ResourcePath<ResourceType.BlockStateDefinition>, BlockStateDefinition?>>>()
            
            loadDefinitions(builder.resolveVanilla("assets/minecraft/blockstates"), "minecraft", vanilla)
            builder.resolve("assets")
                .takeIf(Path::isDirectory)
                ?.listDirectoryEntries()
                ?.forEach { namespaceDir ->
                    val blockstatesDir = namespaceDir.resolve("blockstates")
                    if (blockstatesDir.exists()) {
                        loadDefinitions(blockstatesDir, namespaceDir.name, custom)
                    }
                }
            
            for ((path, definition) in vanilla.awaitAll()) {
                vanillaDefinitions[path] = definition
            }
            
            for ((path, definition) in custom.awaitAll()) {
                customDefinitions[path] = definition
            }
        }
        
        private fun CoroutineScope.loadDefinitions(
            dir: Path, namespace: String,
            dest: MutableList<Deferred<Pair<ResourcePath<ResourceType.BlockStateDefinition>, BlockStateDefinition?>>>
        ) {
            dir.walk()
                .filter { it.extension.equals("json", true) }
                .forEach { file ->
                    dest += async {
                        val path = ResourcePath(
                            ResourceType.BlockStateDefinition,
                            namespace, 
                            file.relativeTo(dir).invariantSeparatorsPathString
                        )
                        path to loadDefinition(file)
                    }
                }
        }
        
    }
    
    /**
     * Writes all custom block state definitions to the resource pack.
     */
    inner class Write : PackTask {
        
        override suspend fun run() = coroutineScope {
            for ((path, definition) in customDefinitions) {
                if (definition == null)
                    continue
                launch { builder.writeJson(path, definition) }
            }
        }
        
    }
    
}