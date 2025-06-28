package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface ItemFilterTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerItemFilterType(name: String, itemFilterType: ItemFilterType<*>) =
        addon.registerItemFilterType(name, itemFilterType)
    
}