package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.options.FuelOptions

class Fuel(val options: FuelOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Fuel>() {
        
        override fun create(material: NovaItem): Fuel {
            return Fuel(FuelOptions.configurable(material))
        }
    
    }
    
}