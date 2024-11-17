package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.layout.item.ItemModelLayoutBuilder
import xyz.xenondevs.nova.resources.layout.item.RequestedItemModelLayout
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

class NovaItemBuilder internal constructor(
    id: ResourceLocation
) : ConfigurableRegistryElementBuilder<NovaItem>(NovaRegistries.ITEM, id) {
    
    private var style: Style = Style.empty()
    private var name: Component? = Component.translatable("item.${id.namespace}.${id.name}")
    private var behaviors: MutableList<ItemBehaviorHolder> = ArrayList()
    private var maxStackSize = 64
    private var craftingRemainingItem: ItemStack? = null
    private var isHidden = false
    private var block: NovaBlock? = null
    private var requestedLayout = RequestedItemModelLayout.DEFAULT
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    /**
     * Sets the style of the item name.
     */
    fun style(style: Style) {
        this.style = style
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(color: TextColor) {
        this.style = Style.style(color)
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(color: TextColor, vararg decorations: TextDecoration) {
        this.style = Style.style(color, *decorations)
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(vararg decorations: TextDecoration) {
        this.style = Style.style(*decorations)
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(decoration: TextDecoration) {
        this.style = Style.style(decoration)
    }
    
    /**
     * Sets the name of the item.
     * Can be set to null to completely hide the tooltip.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component?) {
        this.name = name
    }
    
    /**
     * Sets the localization key of the item.
     *
     * Defaults to `item.<namespace>.<name>`.
     *
     * This function is exclusive with [name].
     */
    fun localizedName(localizedName: String) {
        this.name = Component.translatable(localizedName)
    }
    
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
        this.craftingRemainingItem = item.createItemStack()
    }
    
    /**
     * Sets the crafting remaining item to [material].
     */
    fun craftingRemainingItem(material: Material) {
        this.craftingRemainingItem = ItemStack.of(material)
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
            name?.style(style),
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
    
    internal companion object {
        
        fun fromBlock(id: ResourceLocation, block: NovaBlock): NovaItemBuilder {
            return NovaItemBuilder(id).apply {
                this.block = block
                name(block.name)
                models {
                    selectModel {
                        val scope = BlockModelSelectorScope(block.defaultBlockState, resourcePackBuilder, modelContent)
                        block.requestedLayout.modelSelector.invoke(scope)
                    }
                }
            }
        }
        
    }
    
}