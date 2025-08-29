package xyz.xenondevs.nova.resources.builder.task

import kotlinx.serialization.json.Json
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.ItemModelDefinition
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.util.toString

/**
 * Generates item model definitions.
 */
class ItemModelContent(val builder: ResourcePackBuilder) : PackBuildData {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val modelContent by builder.getBuildDataLazily<ModelContent>()
    
    private val vanillaDefsByPath = HashMap<ResourcePath<ResourceType.ItemModelDefinition>, ItemModelDefinition?>()
    private val customDefsByPath = HashMap<ResourcePath<ResourceType.ItemModelDefinition>, ItemModelDefinition>()
    private val customDefsByDef = HashMap<ItemModelDefinition, HashSet<ResourcePath<ResourceType.ItemModelDefinition>>>()
    
    private var generatedDefCount = 0
    
    /**
     * Registers the given [def] under the given [path].
     */
    operator fun set(path: ResourcePath<ResourceType.ItemModelDefinition>, def: ItemModelDefinition) {
        customDefsByPath[path] = def
        customDefsByDef.getOrPut(def, ::HashSet) += path
    }
    
    /**
     * Retrieves the item model definition registered under the given [path] or null if none exists.
     */
    operator fun get(path: ResourcePath<ResourceType.ItemModelDefinition>): ItemModelDefinition? {
        return customDefsByPath[path] ?: vanillaDefsByPath[path]
    }
    
    /**
     * Retrieves the custom item model definition registered under the given [path] or null if none exists.
     */
    fun getCustom(path: ResourcePath<ResourceType.ItemModelDefinition>): ItemModelDefinition? =
        customDefsByPath[path]
    
    /**
     * Retrieves the vanilla item model definition registered under the given [path] or null if none exists.
     */
    fun getVanilla(path: ResourcePath<ResourceType.ItemModelDefinition>): ItemModelDefinition? =
        vanillaDefsByPath.getOrPut(path) { builder.readJsonVanillaCatching(path, json) }
    
    /**
     * Retrieves the first [ResourcePath] the given [def] is registered under or
     * creates a new [ResourcePath] using [createPath], registers [def] under it and returns it.
     */
    fun getOrPut(
        def: ItemModelDefinition,
        createPath: () -> ResourcePath<ResourceType.ItemModelDefinition>
    ): ResourcePath<ResourceType.ItemModelDefinition> {
        val existingPath = customDefsByDef[def]
        if (existingPath != null)
            return existingPath.first()
        
        val path = createPath()
        set(path, def)
        return path
    }
    
    /**
     * Finds the [ResourcePath] of the given [def] or registers it in `nova:gen_item/`.
     */
    fun getOrPutGenerated(def: ItemModelDefinition): ResourcePath<ResourceType.ItemModelDefinition> {
        val packIdPath = builder.id.toString("/").replace(".", "_.")
        return getOrPut(def) { ResourcePath(ResourceType.ItemModelDefinition, "nova", "gen_item/$packIdPath/${generatedDefCount++}") }
    }
    
    /**
     * Generates item model definition files for registered nova items and writes them to [ItemModelContent].
     */
    inner class GenerateItemDefinitions : PackTask {
        
        override val stage = BuildStage.POST_WORLD // // SelectItemModelProperty.Component requires registry access for serialization
        override val runAfter = setOf(ModelContent.DiscoverAllModels::class)
        override val runBefore = setOf(ModelContent.Write::class, Write::class)
        
        override suspend fun run() {
            for (item in NovaRegistries.ITEM) {
                val definition = ItemModelDefinitionBuilder(
                    builder
                ) { modelSelector ->
                    val scope = ItemModelSelectorScope(item, builder, modelContent)
                    val (model, _) = modelSelector(scope).buildScaled(modelContent)
                    val id = modelContent.getOrPutGenerated(model)
                    modelContent.rememberUsage(id)
                    id
                }.apply(item.configureDefinition).build()
                
                val path = ResourcePath.of(ResourceType.ItemModelDefinition, item.id)
                set(path, definition)
            }
        }
        
    }
    
    /**
     * Writes all item model definitions of [ItemModelContent] to the resource pack.
     */
    inner class Write : PackTask {
        
        override suspend fun run() {
            for ((path, def) in customDefsByPath) {
                builder.writeJson(path, def)
            }
        }
        
    }
    
}