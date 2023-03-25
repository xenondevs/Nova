package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.NovaItem

class FireResistant : ItemBehavior() {
    
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.FIRE_RESISTANT))
    
    companion object : ItemBehaviorFactory<FireResistant>() {
        override fun create(material: NovaItem) = FireResistant()
    }
    
}