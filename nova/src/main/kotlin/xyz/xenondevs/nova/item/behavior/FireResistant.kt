package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.NovaItem

class FireResistant : ItemBehavior() {
    
    override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
        return listOf(VanillaMaterialProperty.FIRE_RESISTANT)
    }
    
    companion object : ItemBehaviorFactory<FireResistant>() {
        override fun create(material: NovaItem) = FireResistant()
    }
    
}