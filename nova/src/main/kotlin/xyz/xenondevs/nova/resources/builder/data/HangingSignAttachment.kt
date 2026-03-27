package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HangingSignAttachment {
    
    @SerialName("wall")
    WALL,
    
    @SerialName("ceiling")
    CEILING,
    
    @SerialName("ceiling_middle")
    CEILING_MIDDLE

}