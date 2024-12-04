package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HeadKind {
    
    @SerialName("skeleton")
    SKELETON,
    
    @SerialName("wither_skeleton")
    WITHER_SKELETON,
    
    @SerialName("player")
    PLAYER,
    
    @SerialName("zombie")
    ZOMBIE,
    
    @SerialName("creeper")
    CREEPER,
    
    @SerialName("piglin")
    PIGLIN,
    
    @SerialName("dragon")
    DRAGON
    
}