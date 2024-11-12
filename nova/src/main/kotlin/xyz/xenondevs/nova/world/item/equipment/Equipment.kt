package xyz.xenondevs.nova.world.item.equipment

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.layout.equipment.EquipmentLayout

/**
 * Represents a custom armor texture.
 */
class Equipment internal constructor(
    val id: ResourceLocation,
    internal val makeLayout: (ResourcePackBuilder) -> EquipmentLayout
)