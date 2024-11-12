package xyz.xenondevs.nova.resources.layout.equipment

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.EquipmentModel
import xyz.xenondevs.nova.resources.layout.equipment.AnimatedEquipmentLayout.Animation
import java.awt.Color

@RegistryElementBuilderDsl
class AnimatedEquipmentLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var layers = HashMap<EquipmentModel.Type, List<AnimatedEquipmentLayout.Layer>>()
    private var cameraOverlay: Animation? = null
    
    /**
     * Defines humanoid equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid/` and `textures/entity/equipment/humanoid_leggings/`.
     */
    fun humanoid(builder: AnimatedEquipmentLayersBuilder.() -> Unit) {
        val humanoidLayers = AnimatedEquipmentLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
        layers[EquipmentModel.Type.HUMANOID] = humanoidLayers
        layers[EquipmentModel.Type.HUMANOID] = humanoidLayers
    }
    
    /**
     * Defines wolf equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/wolf_body/`.
     */
    fun wolfBody(builder: AnimatedEquipmentLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.WOLF_BODY] = AnimatedEquipmentLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines horse equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/horse_body/`.
     */
    fun horseBody(builder: AnimatedEquipmentLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.HORSE_BODY] = AnimatedEquipmentLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines llama equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/llama_body/`.
     */
    fun llamaBody(builder: AnimatedEquipmentLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.LLAMA_BODY] = AnimatedEquipmentLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines elytra equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/elytra/`.
     */
    fun wings(builder: AnimatedEquipmentLayersBuilder.() -> Unit) {
        layers[EquipmentModel.Type.WINGS] = AnimatedEquipmentLayersBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * For each frame, [getCameraOverlay] supplies the textures under `textures/<texture>.png` to be used for the camera overlay,
     * which is an image that will be rendered over the entire screen when the player is wearing the armor and in first person mode.
     */
    @JvmName("cameraOverlayResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun cameraOverlay(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getCameraOverlay: (frame: Int) -> ResourcePath) {
        cameraOverlay = Animation(List(frames, getCameraOverlay), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame, [getCameraOverlay] supplies the textures under `textures/<texture>.png` to be used for the camera overlay,
     * which is an image that will be rendered over the entire screen when the player is wearing the armor and in first person mode.
     */
    @OverloadResolutionByLambdaReturnType
    fun cameraOverlay(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getCameraOverlay: (frame: Int) -> String) {
        cameraOverlay = Animation(List(frames) { ResourcePath.of(getCameraOverlay(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses the textures under `textures/<frame>.png` for the camera overlay, which is an image that will be rendered over the
     * entire screen when the player is wearing the armor and in first person mode.
     */
    fun cameraOverlay(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath) {
        cameraOverlay = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses the textures under `textures/<frame>.png` for the camera overlay, which is an image that will be rendered over the
     * entire screen when the player is wearing the armor and in first person mode.
     */
    fun cameraOverlay(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
        cameraOverlay = Animation(frames.map { ResourcePath.of(it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    internal fun build(): AnimatedEquipmentLayout {
        return AnimatedEquipmentLayout(layers, cameraOverlay)
    }
    
}

@RegistryElementBuilderDsl
class AnimatedEquipmentLayersBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = ArrayList<AnimatedEquipmentLayout.Layer>()
    
    /**
     * Adds a layer to the equipment.
     */
    fun layer(builder: AnimatedEquipmentLayerBuilder.() -> Unit) {
        layers += AnimatedEquipmentLayerBuilder(namespace, resourcePackBuilder).apply(builder).build()
    }
    
    internal fun build(): List<AnimatedEquipmentLayout.Layer> = layers
    
}

@RegistryElementBuilderDsl
class AnimatedEquipmentLayerBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var texture: Animation? = null
    private var emissivityMap: Animation? = null
    private var dyeable: EquipmentModel.Layer.Dyeable? = null
    
    
    /**
     * For each frame ([frames] times), the texture is selected under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`
     * using [getTexture], where `namespace` and `texture` are defined by [texture] and `type` is the current entity type.
     */
    @JvmName("textureResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun texture(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getTexture: (frame: Int) -> ResourcePath) {
        texture = Animation(List(frames, getTexture), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the texture is selected under `textures/entity/equipment/<type>/<texture>.png`
     * using [getTexture], where `texture` is defined by [texture] and `type` is the current entity type.
     */
    @OverloadResolutionByLambdaReturnType
    fun texture(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getTexture: (frame: Int) -> String) {
        texture = Animation(List(frames) { ResourcePath.of(getTexture(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all textures `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where 
     * `namespace` and `texture` are defined by each frame from [frames] and `type` is the current entity type.
     */
    fun texture(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath) {
        texture = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    
    /**
     * Uses all textures `textures/entity/equipment/<type>/<texture>.png`, where
     * `texture` is defined by each frame from [frames] and `type` is the current entity type.
     */
    fun texture(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
        texture = Animation(frames.map { ResourcePath.of(it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the emissivity map is selected under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`
     * using [getEmissivityMap], where `namespace` and `texture` are defined by [emissivityMap] and `type` is the current entity type.
     */
    @JvmName("emissivityMapResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun emissivityMap(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getEmissivityMap: (frame: Int) -> ResourcePath) {
        emissivityMap = Animation(List(frames, getEmissivityMap), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the emissivity map is selected under `textures/entity/equipment/<type>/<texture>.png`
     * using [getEmissivityMap], where `texture` is defined by [emissivityMap] and `type` is the current entity type.
     */
    @OverloadResolutionByLambdaReturnType
    fun emissivityMap(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getEmissivityMap: (frame: Int) -> String) {
        emissivityMap = Animation(List(frames) { ResourcePath.of(getEmissivityMap(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all emissivity maps `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where
     * `namespace` and `texture` are defined by each frame from [frames] and `type` is the current entity type.
     */
    fun emissivityMap(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath) {
        emissivityMap = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all emissivity maps `textures/entity/equipment/<type>/<texture>.png`, where
     * `texture` is defined by each frame from [frames] and `type` is the current entity type.
     */
    fun emissivityMap(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
        emissivityMap = Animation(frames.map { ResourcePath.of(it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Makes the armor layer dyeable, using the [defaultColor] if undyed.
     * Setting [defaultColor] to null makes undyed armor layers invisible.
     */
    fun dyeable(defaultColor: Color?) {
        this.dyeable = EquipmentModel.Layer.Dyeable(defaultColor)
    }
    
    internal fun build(): AnimatedEquipmentLayout.Layer {
        return AnimatedEquipmentLayout.Layer(
            texture ?: throw IllegalArgumentException("Texture not defined"),
            emissivityMap,
            dyeable
        )
    }
    
}