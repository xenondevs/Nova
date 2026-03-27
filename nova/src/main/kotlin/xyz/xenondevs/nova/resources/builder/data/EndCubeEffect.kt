package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EndCubeEffect {
    
    @SerialName("portal")
    PORTAL,
    
    @SerialName("gateway")
    GATEWAY
    
}