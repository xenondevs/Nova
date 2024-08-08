package xyz.xenondevs.nova.data.resources.layout.item

import org.joml.Vector3d
import org.joml.Vector4d
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.model.Model
import xyz.xenondevs.nova.data.resources.builder.model.Model.Direction
import xyz.xenondevs.nova.data.resources.builder.model.Model.Element
import xyz.xenondevs.nova.data.resources.builder.model.Model.Element.Face
import xyz.xenondevs.nova.data.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.data.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl

@RegistryElementBuilderDsl
class ItemModelSelectorScope internal constructor(
    item: NovaItem,
    val resourcePackBuilder: ResourcePackBuilder,
    val modelContent: ModelContent
) {
    
    /**
     * The ID of the item.
     */
    val id = item.id
    
    /**
     * The default model for this item under `namespace:item/name` or a new
     * layered model using the texture under `namespace:item/name`.
     */
    val defaultModel: ModelBuilder by lazy {
        val path = ResourcePath(id.namespace, "item/${id.path}")
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: createLayeredModel(
                // empty layer 0 for the case that this used for leather armor (layer 0 is colored)
                ResourcePath("nova", "item/empty"),
                path
            )
    }
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    fun getModel(path: ResourcePath): ModelBuilder =
        modelContent[path]?.let(::ModelBuilder)
            ?: throw IllegalArgumentException("Model $path does not exist")
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
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
     * Creates a new GUI model using the given [layers] as texture
     * 
     * With [background], the model will have the vanilla inventory background.
     * 
     * With [stretched], the model will be stretched to 18x18 pixels. Due to mip mapping, this requires a 32x32 texture
     * with the actual texture placed at (0, 0) to (18, 18).
     */
    fun createGuiModel(background: Boolean, stretched: Boolean, vararg layers: String): ModelBuilder =
        createGuiModel(background, stretched, *layers.mapToArray { ResourcePath.of(it, id.namespace) })
    
    /**
     * Creates a new GUI model using the given [layers] as texture
     *
     * With [background], the model will have the vanilla inventory background.
     *
     * With [stretched], the model will be stretched to 18x18 pixels. Due to mip mapping, this requires a 32x32 texture
     * with the actual texture placed at (0, 0) to (18, 18).
     */
    fun createGuiModel(background: Boolean, stretched: Boolean, vararg layers: ResourcePath): ModelBuilder {
        if (!background && !stretched)
            return createLayeredModel(*layers)
        
        val textures = HashMap<String, String>()
        val elements = ArrayList<Element>()
        
        if (background) {
            textures["background"] = "nova:item/gui/inventory_part"
        }
        for ((idx, layer) in layers.withIndex()) {
            textures[idx.toString()] = layer.toString()
        }
        
        if (background) {
            elements += Element(
                Vector3d(-1.0, -1.0, -1.0),
                Vector3d(17.0, 17.0, -1.0),
                null,
                mapOf(Direction.SOUTH to Face(Vector4d(0.0, 0.0, 16.0, 16.0), "#background", null, 0, 0)),
                true
            )
        }
        val from = if (stretched) -1.0 else 0.0
        val to = if (stretched) 17.0 else 16.0
        val uv = if (stretched) {
            Vector4d(0.0, 0.0, 9.0, 9.0)
        } else {
            Vector4d(0.0, 0.0, 16.0, 16.0)
        }
        for (idx in layers.indices) {
            elements += Element(
                Vector3d(from, from, (idx.toDouble() / layers.size.toDouble())),
                Vector3d(to, to, (idx.toDouble() / layers.size.toDouble())),
                null,
                mapOf(Direction.SOUTH to Face(uv, "#$idx", null, 0, 0)),
                true
            )
        }
        
        return ModelBuilder(Model(ResourcePath("nova", "item/gui_item"), textures, elements))
    }
    
}