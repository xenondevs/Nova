package xyz.xenondevs.nova.data.resources.layout.item

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.model.Model
import xyz.xenondevs.nova.data.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.data.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl

@RegistryElementBuilderDsl
class ItemModelSelectorScope internal constructor(
    item: NovaItem,
    val modelContent: ModelContent
) {
    
    /**
     * The ID of the item.
     */
    val id = item.id
    
    /**
     * The default model for this item under `namespace:item/name` or a new model
     * with parent `minecraft:item/generated` and `"layer0": "namespace:item/name"`.
     */
    val defaultModel: ModelBuilder by lazy { getModel(ResourcePath(id.namespace, "item/${id.path}")) }
    
    /**
     * Gets the model under the given [path] or creates a new layered model using [path] as the texture.
     */
    fun getModel(path: ResourcePath): ModelBuilder =
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: createLayeredModel(
                // empty layer 0 for the case that this used for leather armor (layer 0 is colored)
                ResourcePath("nova", "item/empty"),
                path
            )
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     * Namespaces are not allowed in the [path] parameter.
     */
    fun getModel(path: String): ModelBuilder =
        getModel(ResourcePath.of(path, id.namespace))
    
    /**
     * Creates a new layered model using the given [layers] as the textures.
     */
    fun createLayeredModel(vararg layers: String): ModelBuilder =
        createLayeredModel(*layers.map { ResourcePath.of(it, id.namespace) }.toTypedArray())
    
    /**
     * Creates a new layered model using the given [layers] as raw paths to the textures.
     */
    fun createLayeredModel(vararg layers: ResourcePath): ModelBuilder = ModelBuilder(
        Model(
            parent = ResourcePath("minecraft", "item/generated"),
            textures = layers.mapIndexed { index, layer -> "layer$index" to layer.toString() }.toMap()
        )
    )
    
    /**
     * Creates a new GUI model using the given [texture] as texture, with the vanilla inventory background
     * if [background] is true and stretched to 18x18 pixels if [stretched] is true.
     */
    fun createGuiModel(texture: String, background: Boolean, stretched: Boolean): ModelBuilder =
        createGuiModel(ResourcePath.of(texture, id.namespace), background, stretched)
    
    /**
     * Creates a new GUI model using the given [texture] as raw path to the texture, with the vanilla inventory background
     * if [background] is true and stretched to 18x18 pixels if [stretched] is true.
     */
    fun createGuiModel(texture: ResourcePath, background: Boolean, stretched: Boolean): ModelBuilder {
        val name = when {
            !background && !stretched -> return createLayeredModel(texture)
            background && !stretched -> "background"
            !background && stretched -> "stretched"
            else -> "background_stretched"
        }
        
        return ModelBuilder(
            Model(
                parent = ResourcePath("nova", "item/gui/$name"),
                textures = mapOf("1" to texture.toString()),
            )
        )
    }
    
}