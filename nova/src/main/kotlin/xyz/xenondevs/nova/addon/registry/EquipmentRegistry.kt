package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.resources.builder.layout.equipment.AnimatedEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.StaticEquipmentLayoutBuilder
import xyz.xenondevs.nova.world.item.Equipment

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface EquipmentRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun equipment(name: String, layout: StaticEquipmentLayoutBuilder.() -> Unit): Equipment =
        addon.equipment(name, layout)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun animatedEquipment(name: String, layout: AnimatedEquipmentLayoutBuilder.() -> Unit): Equipment =
        addon.animatedEquipment(name, layout)
    
}