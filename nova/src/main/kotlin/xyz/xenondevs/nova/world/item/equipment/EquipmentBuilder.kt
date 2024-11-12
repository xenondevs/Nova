package xyz.xenondevs.nova.world.item.equipment

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.layout.equipment.AnimatedEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.layout.equipment.EquipmentLayout
import xyz.xenondevs.nova.resources.layout.equipment.StaticEquipmentLayoutBuilder

class EquipmentBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<Equipment>(NovaRegistries.EQUIPMENT, id) {
    
    private var makeLayout: ((ResourcePackBuilder) -> EquipmentLayout)? = null
    
    /**
     * Configures the texture of the equipment.
     * Exclusive with [animatedTexture].
     */
    fun texture(texture: StaticEquipmentLayoutBuilder.() -> Unit) {
        makeLayout = { StaticEquipmentLayoutBuilder(id.namespace, it).apply(texture).build() }
    }
    
    /**
     * Configures the animated texture of the equipment.
     * Exclusive with [texture].
     */
    fun animatedTexture(texture: AnimatedEquipmentLayoutBuilder.() -> Unit) {
        makeLayout = { AnimatedEquipmentLayoutBuilder(id.namespace, it).apply(texture).build() }
    }
    
    override fun build(): Equipment = Equipment(
        id,
        makeLayout ?: throw IllegalStateException("Armor layout not set")
    )
    
}