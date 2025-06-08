package xyz.xenondevs.nova.resources.builder.layout.equipment

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.EquipmentDefinition
import xyz.xenondevs.nova.resources.builder.layout.equipment.AnimatedEquipmentLayout.Animation
import java.awt.Color

@RegistryElementBuilderDsl
class AnimatedEquipmentLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var layers = HashMap<EquipmentDefinition.Type, List<AnimatedEquipmentLayout.Layer<*>>>()
    private var cameraOverlay: Animation<ResourceType.Texture>? = null
    
    /**
     * Defines humanoid equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid/`.
     *
     * @see humanoidLeggings
     * @see wings
     */
    fun humanoid(builder: AnimatedEquipmentLayersBuilder<ResourceType.HumanoidEquipmentTexture>.() -> Unit) {
        val humanoidLayers = AnimatedEquipmentLayersBuilder(namespace, ResourceType.HumanoidEquipmentTexture, resourcePackBuilder).apply(builder).build()
        layers[EquipmentDefinition.Type.HUMANOID] = humanoidLayers
    }
    
    /**
     * Defines humanoid leggings equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/humanoid_leggings/`.
     *
     * @see humanoid
     * @see wings
     */
    fun humanoidLeggings(builder: AnimatedEquipmentLayersBuilder<ResourceType.HumanoidLegginsEquipmentTexture>.() -> Unit) {
        val humanoidLayers = AnimatedEquipmentLayersBuilder(namespace, ResourceType.HumanoidLegginsEquipmentTexture, resourcePackBuilder).apply(builder).build()
        layers[EquipmentDefinition.Type.HUMANOID_LEGGINGS] = humanoidLayers
    }
    
    /**
     * Defines wolf equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/wolf_body/`.
     */
    fun wolfBody(builder: AnimatedEquipmentLayersBuilder<ResourceType.WolfBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.WOLF_BODY] = AnimatedEquipmentLayersBuilder(namespace, ResourceType.WolfBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines horse equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/horse_body/`.
     */
    fun horseBody(builder: AnimatedEquipmentLayersBuilder<ResourceType.HorseBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.HORSE_BODY] = AnimatedEquipmentLayersBuilder(namespace, ResourceType.HorseBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines llama equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/llama_body/`.
     */
    fun llamaBody(builder: AnimatedEquipmentLayersBuilder<ResourceType.LlamaBodyEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.LLAMA_BODY] = AnimatedEquipmentLayersBuilder(namespace, ResourceType.LlamaBodyEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * Defines elytra equipment layers. Textures are expected to be located under
     * `textures/entity/equipment/elytra/`.
     *
     * @see humanoid
     * @see humanoidLeggings
     */
    fun wings(builder: AnimatedEquipmentLayersBuilder<ResourceType.WingsEquipmentTexture>.() -> Unit) {
        layers[EquipmentDefinition.Type.WINGS] = AnimatedEquipmentLayersBuilder(namespace, ResourceType.WingsEquipmentTexture, resourcePackBuilder).apply(builder).build()
    }
    
    /**
     * For each frame, [getCameraOverlay] supplies the textures under `textures/<texture>.png` to be used for the camera overlay,
     * which is an image that will be rendered over the entire screen when the player is wearing the armor and in first person mode.
     */
    @JvmName("cameraOverlayResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun cameraOverlay(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getCameraOverlay: (frame: Int) -> ResourcePath<ResourceType.Texture>) {
        cameraOverlay = Animation(List(frames, getCameraOverlay), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame, [getCameraOverlay] supplies the textures under `textures/<texture>.png` to be used for the camera overlay,
     * which is an image that will be rendered over the entire screen when the player is wearing the armor and in first person mode.
     */
    @OverloadResolutionByLambdaReturnType
    fun cameraOverlay(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getCameraOverlay: (frame: Int) -> String) {
        cameraOverlay = Animation(List(frames) { ResourcePath.of(ResourceType.Texture, getCameraOverlay(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses the textures under `textures/<frame>.png` for the camera overlay, which is an image that will be rendered over the
     * entire screen when the player is wearing the armor and in first person mode.
     */
    fun cameraOverlay(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath<ResourceType.Texture>) {
        cameraOverlay = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses the textures under `textures/<frame>.png` for the camera overlay, which is an image that will be rendered over the
     * entire screen when the player is wearing the armor and in first person mode.
     */
    fun cameraOverlay(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
        cameraOverlay = Animation(frames.map { ResourcePath.of(ResourceType.Texture, it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    internal fun build(): AnimatedEquipmentLayout {
        return AnimatedEquipmentLayout(layers, cameraOverlay)
    }
    
}

@RegistryElementBuilderDsl
class AnimatedEquipmentLayersBuilder<T : ResourceType.EquipmentTexture> internal constructor(
    private val namespace: String,
    private val textureType: T,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private val layers = ArrayList<AnimatedEquipmentLayout.Layer<T>>()
    
    /**
     * Adds a layer to the equipment.
     */
    fun layer(builder: AnimatedEquipmentLayerBuilder<T>.() -> Unit) {
        layers += AnimatedEquipmentLayerBuilder(namespace, textureType, resourcePackBuilder).apply(builder).build()
    }
    
    internal fun build(): List<AnimatedEquipmentLayout.Layer<T>> = layers
    
}

@RegistryElementBuilderDsl
class AnimatedEquipmentLayerBuilder<T : ResourceType.EquipmentTexture> internal constructor(
    private val namespace: String,
    private val textureType: T,
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    private var texture: Animation<T>? = null
    private var emissivityMap: Animation<T>? = null
    private var dyeable: EquipmentDefinition.Layer.Dyeable? = null
    
    
    /**
     * For each frame ([frames] times), the texture is selected under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`
     * using [getTexture], where `namespace` and `texture` are defined by [texture] and `type` is the current entity type.
     */
    @JvmName("textureResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun texture(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getTexture: (frame: Int) -> ResourcePath<T>) {
        texture = Animation(List(frames, getTexture), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the texture is selected under `textures/entity/equipment/<type>/<texture>.png`
     * using [getTexture], where `texture` is defined by [texture] and `type` is the current entity type.
     */
    @OverloadResolutionByLambdaReturnType
    fun texture(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getTexture: (frame: Int) -> String) {
        texture = Animation(List(frames) { ResourcePath.of(textureType, getTexture(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all textures `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where
     * `namespace` and `texture` are defined by each frame from [frames] and `type` is the current entity type.
     */
    fun texture(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath<T>) {
        texture = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    
    /**
     * Uses all textures `textures/entity/equipment/<type>/<texture>.png`, where
     * `texture` is defined by each frame from [frames] and `type` is the current entity type.
     */
    fun texture(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
        texture = Animation(frames.map { ResourcePath.of(textureType, it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the emissivity map is selected under `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`
     * using [getEmissivityMap], where `namespace` and `texture` are defined by [emissivityMap] and `type` is the current entity type.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    @JvmName("emissivityMapResourcePath")
    @OverloadResolutionByLambdaReturnType
    fun emissivityMap(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getEmissivityMap: (frame: Int) -> ResourcePath<T>) {
//        emissivityMap = Animation(List(frames, getEmissivityMap), ticksPerFrame, interpolationMode)
    }
    
    /**
     * For each frame ([frames] times), the emissivity map is selected under `textures/entity/equipment/<type>/<texture>.png`
     * using [getEmissivityMap], where `texture` is defined by [emissivityMap] and `type` is the current entity type.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    @OverloadResolutionByLambdaReturnType
    fun emissivityMap(frames: Int, ticksPerFrame: Int, interpolationMode: InterpolationMode, getEmissivityMap: (frame: Int) -> String) {
//        emissivityMap = Animation(List(frames) { ResourcePath.of(textureType, getEmissivityMap(it), namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all emissivity maps `assets/<namespace>/textures/entity/equipment/<type>/<texture>.png`, where
     * `namespace` and `texture` are defined by each frame from [frames] and `type` is the current entity type.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    fun emissivityMap(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: ResourcePath<T>) {
//        emissivityMap = Animation(frames.toList(), ticksPerFrame, interpolationMode)
    }
    
    /**
     * Uses all emissivity maps `textures/entity/equipment/<type>/<texture>.png`, where
     * `texture` is defined by each frame from [frames] and `type` is the current entity type.
     */
    @Deprecated(EMISSIVITY_MAP_DEPRECATION)
    fun emissivityMap(ticksPerFrame: Int, interpolationMode: InterpolationMode, vararg frames: String) {
//        emissivityMap = Animation(frames.map { ResourcePath.of(textureType, it, namespace) }, ticksPerFrame, interpolationMode)
    }
    
    /**
     * Makes the armor layer dyeable, using the [defaultColor] if undyed.
     * Setting [defaultColor] to null makes undyed armor layers invisible.
     */
    fun dyeable(defaultColor: Color?) {
        this.dyeable = EquipmentDefinition.Layer.Dyeable(defaultColor)
    }
    
    internal fun build(): AnimatedEquipmentLayout.Layer<T> {
        return AnimatedEquipmentLayout.Layer(
            textureType,
            texture ?: throw IllegalArgumentException("Texture not defined"),
            emissivityMap,
            dyeable
        )
    }
    
}