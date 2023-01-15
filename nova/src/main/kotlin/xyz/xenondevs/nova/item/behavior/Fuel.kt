package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.FuelOptions

class Fuel(val options: FuelOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Fuel>() {
        
        override fun create(material: ItemNovaMaterial): Fuel {
            return Fuel(FuelOptions.configurable(material))
        }
    
    }
    
}