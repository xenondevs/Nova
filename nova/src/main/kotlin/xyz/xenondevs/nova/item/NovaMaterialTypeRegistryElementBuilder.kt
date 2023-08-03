@file:Suppress("UNCHECKED_CAST")

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
abstract class NovaMaterialTypeRegistryElementBuilder<S : NovaMaterialTypeRegistryElementBuilder<S, T>, T : Any>(
    registry: WritableRegistry<in T>,
    id: ResourceLocation,
    defaultLocalizationKey: String
) : ConfigurableRegistryElementBuilder<S, T>(registry, id) {
    
    protected var style: Style = Style.empty()
    protected var name: Component = Component.translatable(defaultLocalizationKey)
    
    /**
     * Sets the style of the item name.
     */
    fun style(style: Style): S {
        this.style = style
        return this as S
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(color: TextColor): S {
        this.style = Style.style(color)
        return this as S
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(color: TextColor, vararg decorations: TextDecoration): S {
        this.style = Style.style(color, *decorations)
        return this as S
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(vararg decorations: TextDecoration): S {
        this.style = Style.style(*decorations)
        return this as S
    }
    
    /**
     * Sets the style of the item name.
     */
    fun style(decoration: TextDecoration): S {
        this.style = Style.style(decoration)
        return this as S
    }
    
    /**
     * Sets the name of the item.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component): S {
        this.name = name
        return this as S
    }
    
    /**
     * Sets the localization key of the item.
     *
     * Defaults to `item.<namespace>.<name>`.
     *
     * This function is exclusive with [name].
     */
    fun localizedName(localizedName: String): S {
        this.name = Component.translatable(localizedName)
        return this as S
    }
    
}