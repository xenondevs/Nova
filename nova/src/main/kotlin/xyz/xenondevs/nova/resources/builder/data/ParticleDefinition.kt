package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

@Serializable
internal data class ParticleDefinition(
    val textures: List<ResourcePath<ResourceType.ParticleTexture>>
)