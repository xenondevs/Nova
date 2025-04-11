package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

@Serializable
enum class HeadKind(val defaultTexture: ResourcePath<ResourceType.EntityTexture>?) {
    
    @SerialName("skeleton")
    SKELETON(ResourcePath.of(ResourceType.EntityTexture, "skeleton/skeleton")),
    
    @SerialName("wither_skeleton")
    WITHER_SKELETON(ResourcePath.of(ResourceType.EntityTexture, "skeleton/wither_skeleton")),
    
    @SerialName("player")
    PLAYER(null),
    
    @SerialName("zombie")
    ZOMBIE(ResourcePath.of(ResourceType.EntityTexture, "zombie/zombie")),
    
    @SerialName("creeper")
    CREEPER(ResourcePath.of(ResourceType.EntityTexture, "creeper/creeper")),
    
    @SerialName("piglin")
    PIGLIN(ResourcePath.of(ResourceType.EntityTexture, "piglin/piglin")),
    
    @SerialName("dragon")
    DRAGON(ResourcePath.of(ResourceType.EntityTexture, "enderdragon/dragon"))
    
}