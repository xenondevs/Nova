package xyz.xenondevs.nova.resources.builder.layout.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

@Serializable
@ApiStatus.Internal
sealed interface EntityVariantLayout {
    
    @Serializable
    @SerialName("simple")
    @ApiStatus.Internal
    class Simple(val texture: ResourcePath<ResourceType.Texture>) : EntityVariantLayout
    
    @Serializable
    @SerialName("wolf")
    @ApiStatus.Internal
    class Wolf(
        val wild: ResourcePath<ResourceType.Texture>,
        val tame: ResourcePath<ResourceType.Texture>,
        val angry: ResourcePath<ResourceType.Texture>,
    ) : EntityVariantLayout
    
}