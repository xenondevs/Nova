package xyz.xenondevs.nova.resources.builder.layout.equipment

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.EquipmentDefinition
import java.awt.Color

@RegistryElementBuilderDsl
class StaticEquipmentLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = HashMap<EquipmentDefinition.Type, List<StaticEquipmentLayout.Layer<*>>>()
    private var cameraOverlay: ResourcePath<ResourceType.Texture>? = null
    
    /**
     * Defines humanoid equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid/`.
     *
     * @see humanoidLeggings
     * @see wings
     */
    fun humanoid(builder: StaticArmorLayersBuilder<ResourceType.HumanoidEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.HUMANOID] = StaticArmorLayersBuilder(namespace, ResourceType.HumanoidEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines humanoid leggings equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid_leggings/`.
     *
     * @see humanoid
     * @see wings
     */
    fun humanoidLeggings(builder: StaticArmorLayersBuilder<ResourceType.HumanoidLegginsEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.HUMANOID_LEGGINGS] = StaticArmorLayersBuilder(namespace, ResourceType.HumanoidLegginsEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines wolf equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/wolf_body/`.
     */
    fun wolfBody(builder: StaticArmorLayersBuilder<ResourceType.WolfBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.WOLF_BODY] = StaticArmorLayersBuilder(namespace, ResourceType.WolfBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines horse equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/horse_body/`.
     */
    fun horseBody(builder: StaticArmorLayersBuilder<ResourceType.HorseBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.HORSE_BODY] = StaticArmorLayersBuilder(namespace, ResourceType.HorseBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines llama equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/llama_body/`.
     */
    fun llamaBody(builder: StaticArmorLayersBuilder<ResourceType.LlamaBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.LLAMA_BODY] = StaticArmorLayersBuilder(namespace, ResourceType.LlamaBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines elytra equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/elytra/`.
     *
     * @see humanoid
     * @see humanoidLeggings
     */
    fun wings(builder: StaticArmorLayersBuilder<ResourceType.WingsEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.WINGS] = StaticArmorLayersBuilder(namespace, ResourceType.WingsEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Sets the camera overlay texture, which is an image that will be rendered over the
     * entire screen when the player is wearing the equipment and in first person mode.
     */
    fun cameraOverlay(overlay: ResourcePath<ResourceType.Texture>) {
        cameraOverlay = overlay
    }
    
    /**
     * Sets the camera overlay texture, which is an image that will be rendered over the
     * entire screen when the player is wearing the equipment and in first person mode.
     */
    fun cameraOverlay(overlay: String) {
        cameraOverlay = ResourcePath.of(ResourceType.Texture, overlay, namespace)
    }
    
    internal fun build(): StaticEquipmentLayout =
        StaticEquipmentLayout(layers, cameraOverlay)
    
}

@RegistryElementBuilderDsl
class StaticArmorLayersBuilder<T : ResourceType.EquipmentTexture> internal constructor(
    private val namespace: String,
    private val textureType: T,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = ArrayList<StaticEquipmentLayout.Layer<T>>()
    
    /**
     * Adds a layer to the equipment.
     */
    fun layer(builder: StaticEquipmentLayerBuilder<T>.() -> Unit) {
        layers += StaticEquipmentLayerBuilder(namespace, textureType, resourcePackBuilder).apply(builder).build()
    }
    
    internal fun build(): List<StaticEquipmentLayout.Layer<T>> = layers
    
}

@RegistryElementBuilderDsl
class StaticEquipmentLayerBuilder<T : ResourceType.EquipmentTexture> internal constructor(
    private val namespace: String,
    private val textureType: T,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var texture: ResourcePath<T>? = null
    private var usePlayerTexture = false
    private var emissivityMap: ResourcePath<T>? = null
    private var dyeable: EquipmentDefinition.Layer.Dyeable? = null
    
    /**
     * Uses the texture under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where
     * `namespace` and `texture` are defined by [texture] and `type` is the current entity type.
     *
     * If [usePlayerTexture] is true, the layer texture is overridden by a texture given by the player.
     */
    fun texture(texture: ResourcePath<T>, usePlayerTexture: Boolean = false) {
        this.texture = texture
        this.usePlayerTexture = usePlayerTexture
    }
    
    /**
     * Uses the texture under `textures/entity/equipment/<type>/<texture>.png`, where
     * `texture` is defined by [texture] and `type` is the current entity type.
     *
     * If [usePlayerTexture] is true, the layer texture is overridden by a texture given by the player.
     */
    fun texture(texture: String, usePlayerTexture: Boolean = false) {
        this.texture = ResourcePath.of(textureType, texture, namespace)
        this.usePlayerTexture = usePlayerTexture
    }
    
    /**
     * Ues the emissivity map under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`.
     * The emissivity map is used to determine the brightness of the layer.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    fun emissivityMap(emissivityMap: ResourcePath<T>) {
//        this.emissivityMap = emissivityMap
    }
    
    /**
     * Ues the emissivity map under `textures/entity/equipment/<type>/<texture>.png`.
     * The emissivity map is used to determine the brightness of the layer.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    fun emissivityMap(emissivityMap: String) {
//        this.emissivityMap = ResourcePath.of(textureType, emissivityMap, namespace)
    }
    
    /**
     * Makes the armor layer dyeable, using the [defaultColor] if undyed.
     * Setting [defaultColor] to null makes undyed armor layers invisible.
     */
    fun dyeable(defaultColor: Color?) {
        this.dyeable = EquipmentDefinition.Layer.Dyeable(defaultColor)
    }
    
    internal fun build(): StaticEquipmentLayout.Layer<T> {
        return StaticEquipmentLayout.Layer(
            textureType,
            texture ?: throw IllegalArgumentException("Texture not defined"),
            usePlayerTexture,
            emissivityMap,
            dyeable
        )
    }
    
}