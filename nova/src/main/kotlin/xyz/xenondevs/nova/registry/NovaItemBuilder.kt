@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

/**
 * A builder for [NovaItem].
 */
@RegistryElementBuilderDsl
sealed interface NovaItemBuilder : ConfigurableBuilder, NameableBuilder, RegistryEntryBuilder.Paper<ItemType> {
    
    /**
     * Sets the block of this [NovaItem], making it placeable.
     * 
     * Also updates this item's [modelDefinition] to use the block's model and changes the [name] to the translation key `block.<block namespace>.<block name>`.
     * If you want to use a different model / name, call [modelDefinition] / [name] after this function.
     * 
     * Note that this function does not need to be called if the [NovaItemBuilder] was already created with a `block` parameter in [Registrar.item].
     */
    fun block(block: RegistryEntry.Paper<BlockType>)
    
    /**
     * Sets the name of the item.
     * Can be set to null to completely hide the tooltip.
     *
     * This function is exclusive with [localizedName].
     */
    @JvmName("nameNullable")
    fun name(name: Component?)
    
    override fun name(name: Component) {
        name(name as Component?)
    }
    
    /**
     * Adds lore lines (tooltip) to the item.
     */
    fun lore(vararg lines: Component)
    
    /**
     * Sets the maximum stack size of the item. Cannot exceed the maximum client-side stack size.
     */
    fun maxStackSize(maxStackSize: Int)
    
    /**
     * Adds to the behaviors of this item to [itemBehaviors].
     */
    fun behaviors(vararg itemBehaviors: ItemBehaviorHolder)
    
    /**
     * Sets the crafting remaining item to [item].
     */
    @JvmName("craftingRemainingItemType")
    fun craftingRemainingItem(item: RegistryEntry.Paper<ItemType>)
    
    /**
     * Sets the crafting remaining item to [item].
     */
    fun craftingRemainingItem(item: TypedKey<ItemType>) {
        craftingRemainingItem(RegistryEntry.paper(item))
    }
    
    /**
     * Configures whether the item is hidden from the give and similar commands.
     * Hidden items also do not have advanced tooltips (`/nova advancedTooltips`).
     *
     * Defaults to `false`.
     *
     * Should be used for gui items.
     */
    fun hidden(hidden: Boolean)
    
    /**
     * Sets the tooltip style of the item.
     */
    fun tooltipStyle(tooltipStyle: RegistryEntry.Nova<TooltipStyle>)
    
    /**
     * Configures the [item model definition](https://minecraft.wiki/w/Items_model_definition) of the item.
     */
    fun modelDefinition(itemModelDefinition: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit)
    
}
