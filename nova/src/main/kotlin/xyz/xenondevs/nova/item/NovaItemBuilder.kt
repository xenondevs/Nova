package xyz.xenondevs.nova.item

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.behavior.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.item.logic.ItemLogic
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock

class NovaItemBuilder internal constructor(
    id: ResourceLocation
): NovaMaterialTypeRegistryElementBuilder<NovaItemBuilder, NovaItem>(NovaRegistries.ITEM, id, "item.${id.namespace}.${id.name}") {
    
    private var behaviors: MutableList<ItemBehaviorHolder> = ArrayList()
    private var maxStackSize = 64
    private var craftingRemainingItem: ItemBuilder? = null
    private var isHidden = false
    private var block: NovaBlock? = null
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Sets the maximum stack size of the item. Cannot exceed the maximum client-side stack size.
     */
    fun maxStackSize(maxStackSize: Int): NovaItemBuilder {
        if (maxStackSize > 64)
            throw IllegalArgumentException("Max stack size cannot exceed 64")
        
        this.maxStackSize = maxStackSize
        return this
    }
    
    /**
     * Sets the behaviors of this item to [itemBehaviors].
     */
    fun behaviors(vararg itemBehaviors: ItemBehaviorHolder): NovaItemBuilder {
        this.behaviors = itemBehaviors.toMutableList()
        return this
    }
    
    /**
     * Adds the specified [itemBehaviors] to the item.
     */
    fun addBehaviors(vararg itemBehaviors: ItemBehaviorHolder): NovaItemBuilder {
        this.behaviors += itemBehaviors
        return this
    }
    
    /**
     * Sets the crafting remaining item to [item].
     */
    fun craftingRemainingItem(item: NovaItem): NovaItemBuilder {
        this.craftingRemainingItem = item.createItemBuilder()
        return this
    }
    
    /**
     * Sets the crafting remaining item to [material].
     */
    fun craftingRemainingItem(material: Material): NovaItemBuilder {
        this.craftingRemainingItem = ItemBuilder(material)
        return this
    }
    
    /**
     * Sets the crafting remaining item to be built using the specified [itemBuilder].
     */
    fun craftingRemainingItem(itemBuilder: ItemBuilder): NovaItemBuilder {
        this.craftingRemainingItem = itemBuilder
        return this
    }
    
    /**
     * Configures whether the item is hidden from the give command.
     * 
     * Defaults to `false`.
     * 
     * Should be used for gui items.
     */
    fun hidden(hidden: Boolean): NovaItemBuilder {
        this.isHidden = hidden
        return this
    }
    
    override fun build(): NovaItem {
        val item = NovaItem(
            id,
            name.style(style),
            style,
            ItemLogic(behaviors),
            maxStackSize,
            craftingRemainingItem,
            isHidden,
            block,
            configId
        )
        block?.item = item
        return item
    }
    
    companion object {
        
        fun fromBlock(block: NovaBlock): NovaItemBuilder {
            return NovaItemBuilder(block.id).apply {
                this.block = block
                name(block.name)
                if (block is NovaTileEntityBlock)
                    behaviors(TileEntityItemBehavior)
            }
        }
        
    }
    
}