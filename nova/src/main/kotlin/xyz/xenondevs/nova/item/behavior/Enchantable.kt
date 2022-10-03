package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.EnchantableOptions

class Enchantable(val options: EnchantableOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Enchantable>() {
        override fun create(material: ItemNovaMaterial) =
            Enchantable(EnchantableOptions.configurable(material))
    }
    
}