package xyz.xenondevs.nova.data.resources.model.layout.item

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.model.Model
import xyz.xenondevs.nova.data.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.data.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.item.NovaItem

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
    val defaultModel: ModelBuilder by lazy { getModelRawPath(ResourcePath(id.namespace, "item/${id.path}")) }
    
    /**
     * Gets the model under the given [path] or creates a new layered model using [path] as the texture.
     */
    fun getModelRawPath(path: ResourcePath): ModelBuilder =
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: createLayeredModelRawPath(path)
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     * Namespaces are not allowed in the [path] parameter.
     *
     * Example path: `my_item`, resolves to the raw path `addon_namespace:item/my_item`.
     */
    fun getModel(path: String): ModelBuilder =
        getModelRawPath(ResourcePath(id.namespace, "item/$path"))
    
    /**
     * Creates a new layered model using the given [layers] as the textures.
     * Each layer is prefixed with `item/`, namespaces are not allowed.
     */
    fun createLayeredModel(vararg layers: String): ModelBuilder =
        createLayeredModelRawPath(*layers.map { ResourcePath.of("item/$it", id.namespace) }.toTypedArray())
    
    /**
     * Creates a new layered model using the given [layers] as raw paths to the textures.
     */
    fun createLayeredModelRawPath(vararg layers: ResourcePath): ModelBuilder = ModelBuilder(
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
        createGuiModelRawPath(ResourcePath.of("item/$texture", id.namespace), background, stretched)
    
    /**
     * Creates a new GUI model using the given [texture] as raw path to the texture, with the vanilla inventory background
     * if [background] is true and stretched to 18x18 pixels if [stretched] is true.
     */
    fun createGuiModelRawPath(texture: ResourcePath, background: Boolean, stretched: Boolean): ModelBuilder {
        val name = when {
            !background && !stretched -> return createLayeredModelRawPath(texture)
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