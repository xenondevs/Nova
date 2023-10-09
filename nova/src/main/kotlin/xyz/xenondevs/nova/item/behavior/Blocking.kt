package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty

object Blocking : ItemBehavior {
    
    override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
        return listOf(VanillaMaterialProperty.SHIELD)
    }
    
}