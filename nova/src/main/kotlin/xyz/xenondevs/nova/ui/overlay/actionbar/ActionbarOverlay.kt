package xyz.xenondevs.nova.ui.overlay.actionbar

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.data.resources.CharSizes

interface ActionbarOverlay {
    
    /**
     * The component to display.
     */
    val component: Component
    
    /**
     * The width of the [component] in pixels.
     */
    fun getWidth(locale: String): Float =
        CharSizes.calculateComponentWidth(component, locale)
    
}