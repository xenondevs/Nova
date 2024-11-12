package xyz.xenondevs.nova.resources.layout.equipment

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.model.EquipmentModel

internal sealed interface EquipmentLayout

internal data class StaticEquipmentLayout(
    val layers: Map<EquipmentModel.Type, List<Layer>>,
    val cameraOverlay: ResourcePath?
) : EquipmentLayout {
    
    fun toEquipmentModel() = EquipmentModel(
        layers.mapValues { (_, layers) ->
            layers.map { layer ->
                EquipmentModel.Layer(layer.texture, layer.usePlayerTexture, layer.dyeable)
            }
        }
    )
    
    data class Layer(
        val texture: ResourcePath,
        val usePlayerTexture: Boolean,
        val emissivityMap: ResourcePath?,
        val dyeable: EquipmentModel.Layer.Dyeable?
    )
    
}

internal data class AnimatedEquipmentLayout(
    val layers: Map<EquipmentModel.Type, List<Layer>>,
    val cameraOverlay: Animation?
) : EquipmentLayout {
    
    internal data class Layer(
        val texture: Animation,
        val emissivityMap: Animation?,
        val dyeable: EquipmentModel.Layer.Dyeable?
    )
    
    internal data class Animation(
        val frames: List<ResourcePath>,
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