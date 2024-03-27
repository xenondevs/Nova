package xyz.xenondevs.nova.item.armor

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.layout.armor.ArmorLayout
import xyz.xenondevs.nova.data.resources.layout.armor.ArmorLayoutBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder

class ArmorBuilder internal constructor(id: ResourceLocation) : RegistryElementBuilder<Armor>(NovaRegistries.ARMOR, id) {
    
    private var layout: ArmorLayout? = null
    
    fun texture(texture: ArmorLayoutBuilder.() -> Unit) {
        layout = ArmorLayoutBuilder(id.namespace).apply(texture).build()
    }
    
    override fun build(): Armor = Armor(
        id,
        layout ?: throw IllegalStateException("Armor layout not set")
    )
    
}