package xyz.xenondevs.nova.registry

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * A builder for something that can have a [Component] as a name.
 */
@RegistryElementBuilderDsl
sealed interface NameableBuilder {
    
    /**
     * Sets the style of the name.
     */
    fun style(style: Style)
    
    /**
     * Sets the style of the name.
     */
    fun style(color: TextColor) {
        style(Style.style(color))
    }
    
    /**
     * Sets the style of the name.
     */
    fun style(color: TextColor, vararg decorations: TextDecoration) {
        style(Style.style(color, *decorations))
    }
    
    /**
     * Sets the style of the name.
     */
    fun style(vararg decorations: TextDecoration) {
        style(Style.style(*decorations))
    }
    
    /**
     * Sets the style of the name.
     */
    fun style(decoration: TextDecoration) {
        style(Style.style(decoration))
    }
    
    /**
     * Sets the name.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component)
    
    /**
     * Sets the localization key.
     *
     * This function is exclusive with [name].
     */
    fun localizedName(localizedName: String) {
        name(Component.translatable(localizedName))
    }
    
}