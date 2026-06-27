package xyz.xenondevs.nova.packetentity

import org.bukkit.attribute.Attribute
import xyz.xenondevs.commons.provider.dsl.DslProperty

/**
 * DSL property for configuring entity attributes.
 *
 * ```kotlin
 * attributes[Attribute.SCALE] by 2.0
 * attributes[Attribute.SCALE] by null // reset to default
 * ```
 */
@PacketEntityDslMarker
sealed interface AttributesDslProperty {
    
    /**
     * Gets the DSL property for [attribute].
     */
    operator fun get(attribute: Attribute): DslProperty<Double?>
    
}
