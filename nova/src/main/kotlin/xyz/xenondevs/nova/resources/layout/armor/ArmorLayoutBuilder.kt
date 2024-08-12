package xyz.xenondevs.nova.resources.layout.armor

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl

internal class ArmorLayout(
    val layer1: ResourcePath?,
    val layer2: ResourcePath?,
    val layer1EmissivityMap: ResourcePath?,
    val layer2EmissivityMap: ResourcePath?,
    val interpolationMode: InterpolationMode,
    val fps: Double
)

@RegistryElementBuilderDsl
class ArmorLayoutBuilder internal constructor(private val namespace: String) {
    
    private var layer1: ResourcePath? = null
    private var layer2: ResourcePath? = null
    private var layer1EmissivityMap: ResourcePath? = null
    private var layer2EmissivityMap: ResourcePath? = null
    private var interpolationMode: InterpolationMode = InterpolationMode.NONE
    private var fps = 0.0
    
    /**
     * Sets the textures [layer1] and [layer2] for the armor model.
     */
    fun texture(layer1: ResourcePath, layer2: ResourcePath) {
        this.layer1 = layer1
        this.layer2 = layer2
    }
    
    /**
     * Sets the textures [layer1] and [layer2] for the armor model.
     */
    fun texture(layer1: String, layer2: String) {
        this.layer1 = ResourcePath.of(layer1, namespace)
        this.layer2 = ResourcePath.of(layer2, namespace)
    }
    
    /**
     * Sets the emissivity maps [layer1] and [layer2] for the armor model.
     */
    fun emissivityMap(layer1: ResourcePath, layer2: ResourcePath) {
        layer1EmissivityMap = layer1
        layer2EmissivityMap = layer2
    }
    
    /**
     * Sets the emissivity maps [layer1] and [layer2] for the armor model.
     */
    fun emissivityMap(layer1: String, layer2: String) {
        layer1EmissivityMap = ResourcePath.of(layer1, namespace)
        layer2EmissivityMap = ResourcePath.of(layer2, namespace)
    }
    
    /**
     * Enables armor texture animation with the given [fps] and [interpolationMode].
     */
    fun animated(fps: Double, interpolationMode: InterpolationMode) {
        this.fps = fps
        this.interpolationMode = interpolationMode
    }
    
    internal fun build(): ArmorLayout =
        ArmorLayout(
            layer1, layer2,
            layer1EmissivityMap, layer2EmissivityMap,
            interpolationMode, fps
        )
    
}

/**
 * The interpolation mode for armor texture animations.
 */
enum class InterpolationMode {
    
    /**
     * Individual animation frames are displayed without any interpolation.
     */
    NONE,
    
    /**
     * A linear interpolation is used to animate between frames.
     */
    LINEAR
    
}