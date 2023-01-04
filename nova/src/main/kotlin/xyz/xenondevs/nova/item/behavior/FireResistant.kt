package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty

object FireResistant : ItemBehavior() {
    
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.FIRE_RESISTANT))
    
}