package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

class NovaItemBuilder internal constructor(
    id: Key
) : ConfigurableRegistryElementBuilder<NovaItem>(NovaRegistries.ITEM, id) {
    
    private var style: Style = Style.empty()
    private var name: Component? = Component.translatable("item.${id.namespace()}.${id.value()}")
    private val lore = ArrayList<Component>()
    private var behaviors: MutableList<ItemBehaviorHolder> = ArrayList()
    private var maxStackSize = 64
    private var craftingRemainingItem: Key? = null
    private var isHidden = false
    private var block: NovaBlock? = null
    private var tooltipStyle: TooltipStyle? = null
    private var configureDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit = ItemModelDefinitionBuilder.DEFAULT_CONFIGURE_ITEM_MODEL_SELECTOR
    
    internal constructor(addon: Addon, name: String) : this(Key(addon, name))
    
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
     * Adds lore lines (tooltip) to the item.
     */
    fun lore(vararg lines: Component) {
        this.lore += lines.map { it.withoutPreFormatting() }
    }
    
    /**
     * Sets the maximum stack size of the item. Cannot exceed the maximum client-side stack size.
     */
    fun maxStackSize(maxStackSize: Int) {
        if (maxStackSize > 99)
            throw IllegalArgumentException("Max stack size cannot exceed 99")
        
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
        this.craftingRemainingItem = item.id
    }
    
    /**
     * Sets the crafting remaining item to [material].
     */
    fun craftingRemainingItem(material: Material) {
        this.craftingRemainingItem = material.key()
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
    
    /**
     * Sets the tooltip style of the item.
     */
    fun tooltipStyle(tooltipStyle: TooltipStyle) {
        this.tooltipStyle = tooltipStyle
    }
    
    /**
     * Configures the [item model definition](https://minecraft.wiki/w/Items_model_definition) of the item.
     */
    fun modelDefinition(itemModelDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit) {
        this.configureDefinition = itemModelDefinition
    }
    
    override fun build(): NovaItem {
        val item = NovaItem(
            id,
            name?.style(style),
            lore,
            style,
            behaviors,
            maxStackSize,
            craftingRemainingItem,
            isHidden,
            block,
            configId,
            tooltipStyle,
            configureDefinition
        )
        block?.item = item
        return item
    }
    
    internal companion object {
        
        fun fromBlock(id: Key, block: NovaBlock): NovaItemBuilder {
            return NovaItemBuilder(id).apply {
                this.block = block
                name(block.name)
                when (val layout = block.layout) {
                    is BlockModelLayout.StateBacked, is BlockModelLayout.SimpleEntityBacked -> {
                        modelDefinition {
                            model = buildModel {
                                val scope = BlockModelSelectorScope(block.defaultBlockState, resourcePackBuilder, modelContent)
                                val selector = when (layout) {
                                    is BlockModelLayout.StateBacked -> layout.modelSelector
                                    is BlockModelLayout.SimpleEntityBacked -> layout.modelSelector
                                    else -> throw UnsupportedOperationException()
                                }
                                selector(scope)
                            }
                        }
                    }
                    
                    else -> Unit
                }
            }
        }
        
    }
    
}