package xyz.xenondevs.nova.world.item

import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleSerializer

/**
 * Represents a custom tooltip texture.
 */
@Serializable(with = TooltipStyleSerializer::class)
class TooltipStyle internal constructor(
    override val entry: RegistryEntry.Nova<TooltipStyle>,
) : NovaRegistryElement<TooltipStyle> {
    override fun toString(): String = key.toString()
}