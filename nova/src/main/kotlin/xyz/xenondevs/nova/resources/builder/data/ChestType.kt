package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName

enum class ChestType {
    
    @SerialName("single")
    SINGLE,
    
    @SerialName("left")
    LEFT,
    
    @SerialName("right")
    RIGHT
    
}