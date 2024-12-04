package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.AnimatedEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.EquipmentLayout
import xyz.xenondevs.nova.resources.builder.layout.equipment.StaticEquipmentLayoutBuilder
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.Equipment

interface EquipmentRegistry : AddonHolder {
    
    fun equipment(name: String, layout: StaticEquipmentLayoutBuilder.() -> Unit): Equipment =
        registerEquipment(name) { StaticEquipmentLayoutBuilder(addon.id, it).apply(layout).build() }
    
    fun animatedEquipment(name: String, layout: AnimatedEquipmentLayoutBuilder.() -> Unit): Equipment =
        registerEquipment(name) { AnimatedEquipmentLayoutBuilder(addon.id, it).apply(layout).build() }
    
    private fun registerEquipment(name: String, makeLayout: (ResourcePackBuilder) -> EquipmentLayout): Equipment {
        val id = Key(addon, name)
        val equipment = Equipment(id, makeLayout)
        NovaRegistries.EQUIPMENT[id] = equipment
        return equipment
    }
    
}