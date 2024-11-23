package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GuiSpriteMcMeta(
    val gui: Gui
) {
    
    @Serializable
    data class Gui(
        val scaling: Scaling
    ) {
        
        @Serializable
        data class Scaling(
            val type: SpriteScalingType,
            val width: Int? = null,
            val height: Int? = null,
            @SerialName("stretch_inner")
            val stretchInner: Boolean = false,
            val border: Border? = null,
        ) {
            
            init {
                require(width != null || type == SpriteScalingType.STRETCH) { "Width is required for TILE and NINE_SLICE" }
                require(height != null || type == SpriteScalingType.STRETCH) { "Height is required for TILE and NINE_SLICE" }
                require(border != null || type != SpriteScalingType.NINE_SLICE) { "Border is required for NINE_SLICE" }
            }
            
            
            @Serializable
            data class Border(
                val left: Int,
                val right: Int,
                val top: Int,
                val bottom: Int
            ) {
                
                constructor(value: Int) : this(value, value, value, value)
                
            }
            
        }
        
    }
    
}

@Serializable
enum class SpriteScalingType {
    
    @SerialName("stretch")
    STRETCH,
    
    @SerialName("tile")
    TILE,
    
    @SerialName("nine_slice")
    NINE_SLICE
    
}