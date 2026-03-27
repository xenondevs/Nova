package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CopperGolemStatuePose {
    
    @SerialName("sitting")
    SITTING,
    
    @SerialName("running")
    RUNNING,
    
    @SerialName("star")
    STAR,
    
    @SerialName("standing")
    STANDING
    
}