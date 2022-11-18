package xyz.xenondevs.nova.material.builder

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry

open class ItemNovaMaterialBuilder internal constructor(addon: Addon, name: String) {
    
    protected val id = NamespacedId(addon, name)
    protected val itemBehaviors = ArrayList<ItemBehaviorHolder<*>>()
    protected open var localizedName = "item.${id.namespace}.$name"
    protected var maxStackSize = 64
    
    fun localizedName(localizedName: String): ItemNovaMaterialBuilder {
        this.localizedName = localizedName
        return this
    }
    
    fun maxStackSize(maxStackSize: Int): ItemNovaMaterialBuilder {
        this.maxStackSize = maxStackSize
        return this
    }
    
    fun itemBehaviors(vararg itemBehaviors: ItemBehaviorHolder<*>): ItemNovaMaterialBuilder {
        this.itemBehaviors += itemBehaviors
        return this
    }
    
    open fun register(): ItemNovaMaterial {
        return NovaMaterialRegistry.register(
            ItemNovaMaterial(id, localizedName, NovaItem(itemBehaviors), maxStackSize)
        )
    }
    
}