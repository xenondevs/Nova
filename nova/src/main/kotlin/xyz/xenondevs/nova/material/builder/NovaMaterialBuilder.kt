package xyz.xenondevs.nova.material.builder

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.material.ItemNovaMaterial

abstract class NovaMaterialBuilder<S: NovaMaterialBuilder<S>> internal constructor(addon: Addon, name: String) {
    
    protected val id = NamespacedId(addon, name)
    protected val itemBehaviors = ArrayList<ItemBehaviorHolder<*>>()
    protected open var localizedName = "item.${id.namespace}.$name"
    protected var maxStackSize = 64
    
    fun localizedName(localizedName: String): S {
        this.localizedName = localizedName
        return getThis()
    }
    
    fun maxStackSize(maxStackSize: Int): S {
        this.maxStackSize = maxStackSize
        return getThis()
    }
    
    fun itemBehaviors(vararg itemBehaviors: ItemBehaviorHolder<*>): S {
        this.itemBehaviors += itemBehaviors
        return getThis()
    }
    
    abstract fun getThis(): S
    abstract fun register(): ItemNovaMaterial
    
}