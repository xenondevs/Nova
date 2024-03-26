package xyz.xenondevs.nova.item

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.model.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.data.resources.model.layout.item.ItemModelLayoutBuilder
import xyz.xenondevs.nova.data.resources.model.layout.item.RequestedItemModelLayout
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.behavior.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock

class NovaItemBuilder internal constructor(
    id: ResourceLocation
) : NovaMaterialTypeRegistryElementBuilder<NovaItem>(NovaRegistries.ITEM, id, "item.${id.namespace}.${id.name}") {
    
    private var behaviors: MutableList<ItemBehaviorHolder> = ArrayList()
    private var maxStackSize = 64
    private var craftingRemainingItem: ItemBuilder? = null
    private var isHidden = false
    private var block: NovaBlock? = null
    private var requestedLayout = RequestedItemModelLayout.DEFAULT
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Sets the maximum stack size of the item. Cannot exceed the maximum client-side stack size.
     */
    fun maxStackSize(maxStackSize: Int) {
        if (maxStackSize > 64)
            throw IllegalArgumentException("Max stack size cannot exceed 64")
        
        this.maxStackSize = maxStackSize
    }
    
    /**
     * Sets the behaviors of this item to [itemBehaviors].
     */
    fun behaviors(vararg itemBehaviors: ItemBehaviorHolder) {
        this.behaviors = itemBehaviors.toMutableList()
    }
    
    /**
     * Sets the crafting remaining item to [item].
     */
    fun craftingRemainingItem(item: NovaItem) {
        this.craftingRemainingItem = item.createItemBuilder()
    }
    
    /**
     * Sets the crafting remaining item to [material].
     */
    fun craftingRemainingItem(material: Material) {
        this.craftingRemainingItem = ItemBuilder(material)
    }
    
    /**
     * Sets the crafting remaining item to be built using the specified [itemBuilder].
     */
    fun craftingRemainingItem(itemBuilder: ItemBuilder) {
        this.craftingRemainingItem = itemBuilder
    }
    
    /**
     * Configures whether the item is hidden from the give command.
     *
     * Defaults to `false`.
     *
     * Should be used for gui items.
     */
    fun hidden(hidden: Boolean) {
        this.isHidden = hidden
    }
    
    fun models(init: ItemModelLayoutBuilder.() -> Unit) {
        val builder = ItemModelLayoutBuilder()
        builder.init()
        requestedLayout = builder.build()
    }
    
    override fun build(): NovaItem {
        val item = NovaItem(
            id,
            name.style(style),
            style,
            behaviors,
            maxStackSize,
            craftingRemainingItem,
            isHidden,
            block,
            configId,
            requestedLayout
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
                models {
                    selectModel {
                        val scope = BlockModelSelectorScope(block.defaultBlockState, modelContent)
                        block.requestedLayout.modelSelector.invoke(scope)
                    }
                }
            }
        }
        
    }
    
}