package xyz.xenondevs.nova.resources.layout.equipment

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.EquipmentModel
import java.awt.Color
import java.lang.IllegalArgumentException

@RegistryElementBuilderDsl
class StaticEquipmentLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = HashMap<EquipmentModel.Type, List<StaticEquipmentLayout.Layer>>()
    private var cameraOverlay: ResourcePath? = null
    
    /**
     * Defines humanoid equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid/` and `textures/entity/equipment/humanoid_leggings/`.
     */
    fun humanoid(builder: StaticArmorLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.HUMANOID] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
        layers[EquipmentModel.Type.HUMANOID_LEGGINGS] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines wolf equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/wolf_body/`.
     */
    fun wolfBody(builder: StaticArmorLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.WOLF_BODY] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines horse equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/horse_body/`.
     */
    fun horseBody(builder: StaticArmorLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.HORSE_BODY] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines llama equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/llama_body/`.
     */
    fun llamaBody(builder: StaticArmorLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.LLAMA_BODY] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines elytra equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/elytra/`.
     */
    fun wings(builder: StaticArmorLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.WINGS] = StaticArmorLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Sets the camera overlay texture, which is an image that will be rendered over the
     * entire screen when the player is wearing the equipment and in first person mode.
     */
    fun cameraOverlay(overlay: ResourcePath) {
        cameraOverlay = overlay
    }
    
    /**
     * Sets the camera overlay texture, which is an image that will be rendered over the
     * entire screen when the player is wearing the equipment and in first person mode.
     */
    fun cameraOverlay(overlay: String) {
        cameraOverlay = ResourcePath.of(overlay, namespace)
    }
    
    internal fun build(): StaticEquipmentLayout =
        StaticEquipmentLayout(layers, cameraOverlay)
    
}

@RegistryElementBuilderDsl
class StaticArmorLayersBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = ArrayList<StaticEquipmentLayout.Layer>()
    
    /**
     * Adds a layer to the equipment.
     */
    fun layer(builder: StaticEquipmentLayerBuilder.() -> Unit) {
        layers += StaticEquipmentLayerBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    internal fun build(): List<StaticEquipmentLayout.Layer> = layers
    
}

@RegistryElementBuilderDsl
class StaticEquipmentLayerBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var texture: ResourcePath? = null
    private var usePlayerTexture = false
    private var emissivityMap: ResourcePath? = null
    private var dyeable: EquipmentModel.Layer.Dyeable? = null
    
    /**
     * Uses the texture under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where
     * `namespace` and `texture` are defined by [texture] and `type` is the current entity type.
     * 
     * If [usePlayerTexture] is true, the layer texture is overridden by a texture given by the player.
     */
    fun texture(texture: ResourcePath, usePlayerTexture: Boolean = false) {
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
        this.texture = ResourcePath.of(texture, namespace)
        this.usePlayerTexture = usePlayerTexture
    }
    
    /**
     * Ues the emissivity map under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`.
     * The emissivity map is used to determine the brightness of the layer.
     */
    fun emissivityMap(emissivityMap: ResourcePath) {
        this.emissivityMap = emissivityMap
    }
    
    /**
     * Ues the emissivity map under `textures/entity/equipment/<type>/<texture>.png`.
     * The emissivity map is used to determine the brightness of the layer.
     */
    fun emissivityMap(emissivityMap: String) {
        this.emissivityMap = ResourcePath.of(emissivityMap, namespace)
    }
    
    /**
     * Makes the armor layer dyeable, using the [defaultColor] if undyed.
     * Setting [defaultColor] to null makes undyed armor layers invisible.
     */
    fun dyeable(defaultColor: Color?) {
        this.dyeable = EquipmentModel.Layer.Dyeable(defaultColor)
    }
    
    internal fun build(): StaticEquipmentLayout.Layer {
        return StaticEquipmentLayout.Layer(
            texture ?: throw IllegalArgumentException("Texture not defined"),
            usePlayerTexture,
            emissivityMap,
            dyeable
        )
    }
    
}