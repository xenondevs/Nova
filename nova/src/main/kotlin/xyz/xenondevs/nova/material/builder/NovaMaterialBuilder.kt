package xyz.xenondevs.nova.material.builder

import org.bukkit.Material
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.material.NovaItem

abstract class NovaMaterialBuilder<S: NovaMaterialBuilder<S>> internal constructor(addon: Addon, name: String) {
    
    protected val id = NamespacedId(addon, name)
    protected val itemBehaviors = ArrayList<ItemBehaviorHolder<*>>()
    protected open var localizedName = "item.${id.namespace}.$name"
    protected var maxStackSize = 64
    protected var craftingRemainingItem: ItemBuilder? = null
    protected var isHidden = false
    
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
    
    fun craftingRemainingItem(material: NovaItem): S {
        this.craftingRemainingItem = material.createItemBuilder()
        return getThis()
    }
    
    fun craftingRemainingItem(material: Material): S {
        this.craftingRemainingItem = ItemBuilder(material)
        return getThis()
    }
    
    fun craftingRemainingItem(itemBuilder: ItemBuilder): S {
        this.craftingRemainingItem = itemBuilder
        return getThis()
    }
    
    fun hidden(hidden: Boolean): S {
        this.isHidden = hidden
        return getThis()
    }
    
    abstract fun getThis(): S
    abstract fun register(): NovaItem
    
}