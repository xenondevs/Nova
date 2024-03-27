package xyz.xenondevs.nova.item.armor

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.layout.armor.ArmorLayout

/**
 * Represents a type of custom armor.
 */
class Armor internal constructor(
    val id: ResourceLocation,
    internal val layout: ArmorLayout
)