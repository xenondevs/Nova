package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.world.item.armor.Armor
import xyz.xenondevs.nova.world.item.armor.ArmorBuilder
import xyz.xenondevs.nova.util.ResourceLocation

interface ArmorRegistry : AddonGetter {
    
    fun armor(name: String, layout: ArmorBuilder.() -> Unit): Armor =
        ArmorBuilder(ResourceLocation(addon, name)).apply(layout).register()
    
}