@file:UseSerializers(ColorAsIntSerializer::class)

package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import xyz.xenondevs.nova.serialization.kotlinx.ColorAsIntSerializer
import java.awt.Color

@Serializable
sealed interface TintSource {
    
    @Serializable
    @SerialName("minecraft:constant")
    class Constant(val value: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:dye")
    class Dye(val default: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:grass")
    class Grass(
        val temperature: Double,
        val downfall: Double
    ) : TintSource {
        init {
            require(temperature in 0.0..1.0) { "Temperature must be in range 0.0..1.0" }
            require(downfall in 0.0..1.0) { "Downfall must be in range 0.0..1.0" }
        }
    }
    
    @Serializable
    @SerialName("minecraft:firework")
    class Firework(val default: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:potion")
    class Potion(val default: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:map_color")
    class MapColor(val default: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:team")
    class Team(val default: Color) : TintSource
    
    @Serializable
    @SerialName("minecraft:custom_model_data")
    class CustomModelData(val default: Color, val index: Int = 0) : TintSource {
        init {
            require(index >= 0) { "Index must be >= 0" }
        }
    }
    
}