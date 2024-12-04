package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TimeSource {
    
    @SerialName("daytime")
    DAYTIME,
    
    @SerialName("moon_phase")
    MOON_PHASE,
    
    @SerialName("random")
    RANDOM
    
}