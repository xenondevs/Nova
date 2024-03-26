package xyz.xenondevs.nova.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.NovaBlockBuilder

/**
 * The super-class for [NovaItemBuilder] and [NovaBlockBuilder]
 */
abstract class NovaMaterialTypeRegistryElementBuilder<T : Any>(
    registry: WritableRegistry<in T>,
    id: ResourceLocation,
    defaultLocalizationKey: String
) : ConfigurableRegistryElementBuilder<T>(registry, id) {
    
    protected var style: Style = Style.empty()
    protected var name: Component = Component.translatable(defaultLocalizationKey)
    
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
    fun style(decoration: TextDecoration){
        this.style = Style.style(decoration)
    }
    
    /**
     * Sets the name of the item.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component) {
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
    
}