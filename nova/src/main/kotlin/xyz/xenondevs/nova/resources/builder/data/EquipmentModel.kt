@file:UseSerializers(ColorAsIntSerializer::class)

package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.serialization.kotlinx.ColorAsIntSerializer
import java.awt.Color

@Serializable
internal class EquipmentModel(
    val layers: Map<Type, List<Layer>>
) {
    
    @Serializable
    class Layer(
        val texture: ResourcePath<ResourceType.EquipmentTexture>,
        @SerialName("use_player_texture")
        val usePlayerTexture: Boolean = false,
        val dyeable: Dyeable? = null,
    ) {
        
        @Serializable
        class Dyeable(
            @SerialName("color_when_undyed")
            val defaultColor: Color? = null
        )
        
    }
    
    enum class Type {
        
        @SerialName("horse_body")
        HORSE_BODY,
        
        @SerialName("humanoid")
        HUMANOID,
        
        @SerialName("humanoid_leggings")
        HUMANOID_LEGGINGS,
        
        @SerialName("llama_body")
        LLAMA_BODY,
        
        @SerialName("wings")
        WINGS,
        
        @SerialName("wolf_body")
        WOLF_BODY;
        
        override fun toString() = name.lowercase()
        
    }
    
}