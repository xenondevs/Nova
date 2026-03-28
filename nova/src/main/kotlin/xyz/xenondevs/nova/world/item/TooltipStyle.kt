package xyz.xenondevs.nova.world.item

import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleSerializer

/**
 * Serializable type alias for `RegistryEntry.Nova<TooltipStyle>` using [NovaTooltipStyleEntrySerializer].
 */
typealias NovaTooltipStyleEntry = @Serializable(with = TooltipStyleEntrySerializer::class) RegistryEntry.Nova<TooltipStyle>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<TooltipStyle>` using [NovaTooltipStyleEntrySetSerializer].
 */
typealias NovaTooltipStyleEntrySet = @Serializable(with = TooltipStyleEntrySetSerializer::class) RegistryEntrySet.Nova<TooltipStyle>

/**
 * Represents a custom tooltip texture.
 */
@Serializable(with = TooltipStyleSerializer::class)
class TooltipStyle internal constructor(
    override val entry: RegistryEntry.Nova<TooltipStyle>,
) : NovaRegistryElement<TooltipStyle> {
    override fun toString(): String = key.toString()
}