package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.options.FuelOptions

class Fuel(val options: FuelOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Fuel>() {
        
        override fun create(item: NovaItem): Fuel {
            return Fuel(FuelOptions.configurable(item))
        }
    
    }
    
}