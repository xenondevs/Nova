package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.layout.equipment.EquipmentLayout

/**
 * Represents a custom armor texture.
 */
class Equipment internal constructor(
    val id: Key,
    internal val makeLayout: (ResourcePackBuilder) -> EquipmentLayout
)