package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty

/**
 * Makes items fire-resistant.
 */
object FireResistant : ItemBehavior {
    
    override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
        return listOf(VanillaMaterialProperty.FIRE_RESISTANT)
    }
    
}