package xyz.xenondevs.nova.material.builder

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.ItemLogic
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.material.NovaBlock
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.TileEntityNovaBlock
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.set

class NovaItemBuilder internal constructor(private val id: ResourceLocation) {
    
    private var logic: MutableList<ItemBehaviorHolder<*>> = ArrayList()
    private var localizedName = "item.${id.namespace}.${id.name}"
    private var maxStackSize = 64
    private var craftingRemainingItem: ItemBuilder? = null
    private var isHidden = false
    private var block: NovaBlock? = null
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon.description.id, name))
    
    fun localizedName(localizedName: String): NovaItemBuilder {
        this.localizedName = localizedName
        return this
    }
    
    fun maxStackSize(maxStackSize: Int): NovaItemBuilder {
        this.maxStackSize = maxStackSize
        return this
    }
    
    fun behaviors(vararg itemBehaviors: ItemBehaviorHolder<*>): NovaItemBuilder {
        this.logic = itemBehaviors.toMutableList()
        return this
    }
    
    fun addBehavior(vararg itemBehaviors: ItemBehaviorHolder<*>): NovaItemBuilder {
        this.logic += itemBehaviors
        return this
    }
    
    fun craftingRemainingItem(material: NovaItem): NovaItemBuilder {
        this.craftingRemainingItem = material.createItemBuilder()
        return this
    }
    
    fun craftingRemainingItem(material: Material): NovaItemBuilder {
        this.craftingRemainingItem = ItemBuilder(material)
        return this
    }
    
    fun craftingRemainingItem(itemBuilder: ItemBuilder): NovaItemBuilder {
        this.craftingRemainingItem = itemBuilder
        return this
    }
    
    fun hidden(hidden: Boolean): NovaItemBuilder {
        this.isHidden = hidden
        return this
    }
    
    fun register(): NovaItem {
        val item = NovaItem(
            id,
            localizedName,
            ItemLogic(logic),
            maxStackSize,
            craftingRemainingItem,
            isHidden,
            block
        )
        block?.item = item
        NovaRegistries.ITEM[id] = item
        return item
    }
    
    companion object {
        
        fun fromBlock(block: NovaBlock): NovaItemBuilder {
            return NovaItemBuilder(block.id).apply {
                this.block = block
                localizedName(block.localizedName)
                if (block is TileEntityNovaBlock)
                    behaviors(TileEntityItemBehavior())
            }
        }
        
    }
    
}