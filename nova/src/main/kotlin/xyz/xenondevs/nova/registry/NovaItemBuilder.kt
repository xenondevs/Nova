@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.text.Component
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
sealed interface NovaItemBuilder : ConfigurableBuilder, NameableBuilder, RegistryEntryBuilder.Nova<NovaItem> {
    
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
     * Sets the behaviors of this item to [itemBehaviors].
     */
    fun behaviors(vararg itemBehaviors: ItemBehaviorHolder)
    
    /**
     * Sets the crafting remaining item to [item].
     */
    @JvmName("craftingRemainingNovaItem")
    fun craftingRemainingItem(item: RegistryEntry.Nova<NovaItem>)
    
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
     * Configures whether the item is hidden from the give command.
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