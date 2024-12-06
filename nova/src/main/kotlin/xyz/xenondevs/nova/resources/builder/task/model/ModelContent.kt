package xyz.xenondevs.nova.resources.builder.task.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.util.data.readJson
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * A [PackTaskHolder] that deals with item/block model files.
 * Everything related to item/block model files should run through this.
 */
class ModelContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder, Iterable<Map.Entry<ResourcePath<ResourceType.Model>, Model>> {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val vanillaModelsByPath = HashMap<ResourcePath<ResourceType.Model>, Model?>(4096) // 3691 as of 1.20.2
    private val vanillaModelsByModel = HashMap<Model, HashSet<ResourcePath<ResourceType.Model>>>(4096)
    private val customModelsByPath = HashMap<ResourcePath<ResourceType.Model>, Model>(4096)
    private val customModelsByModel = HashMap<Model, HashSet<ResourcePath<ResourceType.Model>>>(4096)
    private val rememberedUsages = HashSet<ResourcePath<ResourceType.Model>>()
    private var generatedModelCount = 0
    
    /**
     * Sets the custom [model] under the given [path], replacing any existing model under that [path].
     */
    operator fun set(path: ResourcePath<ResourceType.Model>, model: Model) {
        customModelsByPath[path] = model
        customModelsByModel.getOrPut(model, ::HashSet) += path
    }
    
    /**
     * Retrieves the model with the specified [path] or null if it does not exist.
     */
    operator fun get(path: ResourcePath<ResourceType.Model>): Model? =
        getCustom(path) ?: getVanilla(path)
    
    /**
     * Retrieves the custom model with the specified [path] or null if it does not exist.
     */
    fun getCustom(path: ResourcePath<ResourceType.Model>): Model? =
        customModelsByPath[path]
    
    /**
     * Retrieves the vanilla model with the specified [path] or null if it does not exist.
     *
     * Vanilla models are models that are shipped with the game and not part of the resource pack.
     */
    fun getVanilla(path: ResourcePath<ResourceType.Model>): Model? =
        vanillaModelsByPath.getOrPut(path) { loadModel(builder.resolveVanilla(path)) }
    
    /**
     * Retrieves the [Model] registered under the given [path] or creates a model using [createModel],
     * registers it under the given [path] and returns it.
     */
    fun getOrPut(path: ResourcePath<ResourceType.Model>, createModel: () -> Model): Model {
        val existingModel = customModelsByPath[path]
        if (existingModel != null)
            return existingModel
        
        val model = createModel()
        set(path, model)
        return model
    }
    
    /**
     * Retrieves the [ResourcePaths][ResourcePath] the given [model] is registered under or null
     * if it is neither present in the resource pack nor in the vanilla assets.
     */
    fun getPaths(model: Model): Collection<ResourcePath<ResourceType.Model>> {
        return buildList {
            customModelsByModel[model]?.also(::addAll)
            vanillaModelsByModel[model]?.also(::addAll)
        }
    }
    
    /**
     * Retrieves the first [ResourcePath] the given [model] is registered under or
     * creates a new [ResourcePath] using [createPath], registers it under the given [model] and returns it.
     */
    fun getOrPut(model: Model, createPath: () -> ResourcePath<ResourceType.Model>): ResourcePath<ResourceType.Model> {
        val existingPath = customModelsByModel[model]
        if (existingPath != null)
            return existingPath.first()
        
        val path = createPath()
        set(path, model)
        return path
    }
    
    /**
     * Finds the [ResourcePath] of the given [model] or registers it in `nova:gen_model/`.
     */
    fun getOrPutGenerated(model: Model): ResourcePath<ResourceType.Model> {
        return getOrPut(model) { ResourcePath(ResourceType.Model, "nova", "gen_model/${generatedModelCount++}") }
    }
    
    /**
     * Remembers the usage of the model under the given [path] and its parents.
     * Only models that are used will be written to the resource pack.
     */
    fun rememberUsage(path: ResourcePath<ResourceType.Model>) {
        var parentPath: ResourcePath<ResourceType.Model>? = path
        while (parentPath != null) {
            rememberedUsages += parentPath
            parentPath = get(parentPath)?.parent
        }
    }
    
    /**
     * Returns an iterator over the custom models.
     */
    override fun iterator(): Iterator<Map.Entry<ResourcePath<ResourceType.Model>, Model>> = customModelsByPath.iterator()
    
    @PackTask(runAfter = ["ExtractTask#extractAll"])
    private suspend fun discoverAllModels() = coroutineScope {
        val customModels = ArrayList<List<Deferred<Pair<ResourcePath<ResourceType.Model>, Model>>>>()
        
        // discover existing models (base packs)
        ResourcePackBuilder.ASSETS_DIR.listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach { customModels += discoverModels(it.resolve("models"), it.name) }
        
        // discover models from asset packs
        for (assetPack in builder.assetPacks) {
            val modelsDir = assetPack.modelsDir ?: continue
            customModels += discoverModels(modelsDir, assetPack.namespace)
        }
        
        customModels.forEach {
            it.awaitAll().forEach { (path, model) ->
                customModelsByPath[path] = model
                customModelsByModel.getOrPut(model, ::HashSet) += path
            }
        }
    }
    
    private fun CoroutineScope.discoverModels(modelsDir: Path, namespace: String): List<Deferred<Pair<ResourcePath<ResourceType.Model>, Model>>> =
        modelsDir.walk()
            .filter { it.extension.equals("json", true) }
            .mapTo(ArrayList()) { file ->
                async {
                    val path = ResourcePath(ResourceType.Model, namespace, file.relativeTo(modelsDir).invariantSeparatorsPathString.substringBeforeLast('.'))
                    return@async path to (loadModel(file) ?: Model())
                }
            }
    
    private fun loadModel(path: Path): Model? {
        if (path.notExists())
            return null
        
        try {
            return path.readJson<Model>(json)
        } catch (e: Exception) {
            LOGGER.warn("Failed to parse model file $path", e)
        }
        
        return null
    }
    
    @PackTask(runAfter = ["ModelContent#discoverAllModels"])
    private suspend fun write() = coroutineScope {
        for ((id, model) in customModelsByPath) {
            if (id !in rememberedUsages)
                continue
            
            launch {
                builder.writeJson(id, model)
            }
        }
    }
    
}