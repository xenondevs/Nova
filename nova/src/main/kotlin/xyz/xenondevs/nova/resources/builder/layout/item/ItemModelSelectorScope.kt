package xyz.xenondevs.nova.resources.builder.layout.item

import org.joml.Vector3d
import org.joml.Vector4d
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.Model.Direction
import xyz.xenondevs.nova.resources.builder.model.Model.Element
import xyz.xenondevs.nova.resources.builder.model.Model.Element.Face
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.world.item.NovaItem

@RegistryElementBuilderDsl
class ItemModelSelectorScope internal constructor(
    item: NovaItem,
    val resourcePackBuilder: ResourcePackBuilder,
    val modelContent: ModelContent
) : ModelSelectorScope {
    
    /**
     * The ID of the item.
     */
    val id = item.id
    
    /**
     * The default model for this item under `namespace:item/name` or a new
     * layered model using the texture under `namespace:item/name`.
     */
    override val defaultModel: ModelBuilder by lazy {
        val path = ResourcePath(ResourceType.Model, id.namespace(), "item/${id.value()}")
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: createLayeredModel(path)
    }
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    override fun getModel(path: ResourcePath<ResourceType.Model>): ModelBuilder =
        modelContent[path]?.let(::ModelBuilder)
            ?: throw IllegalArgumentException("Model $path does not exist")
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    override fun getModel(path: String): ModelBuilder =
        getModel(ResourcePath.of(ResourceType.Model, path, id.namespace()))
    
    /**
     * Creates a new layered model using the given [layers] as the textures.
     */
    fun createLayeredModel(vararg layers: String): ModelBuilder =
        createLayeredModel(*layers.map { ResourcePath.of(ResourceType.Model, it, id.namespace()) }.toTypedArray())
    
    /**
     * Creates a new layered model using the given [layers] as raw paths to the textures.
     */
    fun createLayeredModel(vararg layers: ResourcePath<ResourceType.Model>): ModelBuilder = ModelBuilder(
        Model(
            parent = ResourcePath(ResourceType.Model, "minecraft", "item/generated"),
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
     * 
     * Using [display], additional transformations can be applied.
     */
    fun createGuiModel(background: Boolean, stretched: Boolean, vararg layers: String, display: Model.Display.Entry? = null): ModelBuilder =
        createGuiModel(background, stretched, *layers.mapToArray { ResourcePath.of(ResourceType.Model, it, id.namespace()) }, display = display)
    
    /**
     * Creates a new GUI model using the given [layers] as texture
     *
     * With [background], the model will have the vanilla inventory background.
     *
     * With [stretched], the model will be stretched to 18x18 pixels. Due to mip mapping, this requires a 32x32 texture
     * with the actual texture placed at (0, 0) to (18, 18).
     * 
     * Using [display], additional transformations can be applied.
     */
    fun createGuiModel(background: Boolean, stretched: Boolean, vararg layers: ResourcePath<ResourceType.Model>, display: Model.Display.Entry? = null): ModelBuilder {
        if (!background && !stretched && display == null)
            return createLayeredModel(*layers)
        
        val elements = ArrayList<Element>()
        if (background) {
            elements += Element(
                Vector3d(-1.0, -1.0, -1.0),
                Vector3d(17.0, 17.0, -1.0),
                null,
                mapOf(Direction.SOUTH to Face(Vector4d(0.0, 0.0, 16.0, 16.0), "#background")),
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
            // name first layer "particle", to stop the client from complaining about missing particle textures
            val name = if (idx == 0) "particle" else idx.toString()
            elements += Element(
                Vector3d(from, from, (idx.toDouble() / layers.size.toDouble())),
                Vector3d(to, to, (idx.toDouble() / layers.size.toDouble())),
                null,
                mapOf(Direction.SOUTH to Face(uv, name)),
                true
            )
        }
        
        val parent = Model(
            ResourcePath(ResourceType.Model, "nova", "item/gui_item"),
            elements = elements,
            display = display?.let { Model.Display(gui = it) }
        )
        val parentId = modelContent.getOrPutGenerated(parent)
        
        val textures = HashMap<String, String>()
        if (background) {
            textures["background"] = "nova:item/gui/inventory_part"
        }
        for ((idx, layer) in layers.withIndex()) {
            val name = if (idx == 0) "particle" else idx.toString()
            textures[name] = layer.toString()
        }
        val model = Model(parentId, textures)
        
        return ModelBuilder(model)
    }
    
}