package xyz.xenondevs.nova.material.builder

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry

open class ItemNovaMaterialBuilder internal constructor(addon: Addon, name: String) : NovaMaterialBuilder<ItemNovaMaterialBuilder>(addon, name) {
    
    override fun getThis() = this
    
    override fun register(): ItemNovaMaterial {
        return NovaMaterialRegistry.register(
            ItemNovaMaterial(id, localizedName, NovaItem(itemBehaviors), maxStackSize)
        )
    }
    
}