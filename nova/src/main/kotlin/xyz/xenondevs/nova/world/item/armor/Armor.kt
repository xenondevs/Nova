package xyz.xenondevs.nova.world.item.armor

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.resources.layout.armor.ArmorLayout

/**
 * Represents a type of custom armor.
 */
class Armor internal constructor(
    val id: ResourceLocation,
    internal val layout: ArmorLayout
)