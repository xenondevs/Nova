package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CompassTarget {
    
    @SerialName("spawn")
    SPAWN,
    
    @SerialName("lodestone")
    LODESTONE,
    
    @SerialName("recovery")
    RECOVERY,
    
    @SerialName("none")
    NONE
    
}