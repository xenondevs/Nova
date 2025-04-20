package xyz.xenondevs.nova.resources.builder.layout.equipment

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.data.EquipmentDefinition

const val EMISSIVITY_MAP_DEPRECATION = "Emissivity maps do no longer work due to changes to core shaders and will likely be removed in a future version."

internal sealed interface EquipmentLayout

internal data class StaticEquipmentLayout(
    val types: Map<EquipmentDefinition.Type, List<Layer<*>>>,
    val cameraOverlay: ResourcePath<ResourceType.Texture>?
) : EquipmentLayout {
    
    fun toEquipmentModel() = EquipmentDefinition(
        types.mapValues { (_, layers) ->
            layers.map { layer ->
                EquipmentDefinition.Layer(layer.texture, layer.usePlayerTexture, layer.dyeable)
            }
        }
    )
    
    data class Layer<out T : ResourceType.EquipmentTexture>(
        val resourceType: T,
        val texture: ResourcePath<T>,
        val usePlayerTexture: Boolean,
        val emissivityMap: ResourcePath<T>?,
        val dyeable: EquipmentDefinition.Layer.Dyeable?
    )
    
}

internal data class AnimatedEquipmentLayout(
    val types: Map<EquipmentDefinition.Type, List<Layer<*>>>,
    val cameraOverlay: Animation<ResourceType.Texture>?
) : EquipmentLayout {
    
    internal data class Layer<out T : ResourceType.EquipmentTexture>(
        val resourceType: T,
        val texture: Animation<T>,
        val emissivityMap: Animation<T>?,
        val dyeable: EquipmentDefinition.Layer.Dyeable?
    )
    
    internal data class Animation<out T : ResourceType.Texture>(
        val frames: List<ResourcePath<T>>,
        val ticksPerFrame: Int,
        val interpolationMode: InterpolationMode
    ) {
        
        init {
            require(frames.isNotEmpty()) { "Frame count must be greater than 0" }
            require(ticksPerFrame > 0) { "Ticks per frame must be greater than 0" }
        }
        
    }
    
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