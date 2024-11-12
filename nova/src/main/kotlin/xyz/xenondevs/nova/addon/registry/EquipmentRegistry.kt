package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.world.item.equipment.Equipment
import xyz.xenondevs.nova.world.item.equipment.EquipmentBuilder

interface EquipmentRegistry : AddonHolder {
    
    fun equipment(name: String, layout: EquipmentBuilder.() -> Unit): Equipment =
        EquipmentBuilder(ResourceLocation(addon, name)).apply(layout).register()
    
}