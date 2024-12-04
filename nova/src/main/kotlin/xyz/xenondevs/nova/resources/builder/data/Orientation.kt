package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Orientation {
    
    @SerialName("north")
    NORTH,
    
    @SerialName("east")
    EAST,
    
    @SerialName("south")
    SOUTH,
    
    @SerialName("west")
    WEST,
    
    @SerialName("up")
    UP,
    
    @SerialName("down")
    DOWN
    
}