package xyz.xenondevs.nova.ui.overlay.actionbar

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.data.resources.CharSizes

interface ActionbarOverlay {
    
    /**
     * The components to display.
     */
    val components: Array<BaseComponent>
    
    /**
     * The width of the [components] in pixels.
     */
    fun getWidth(locale: String): Int =
        CharSizes.calculateComponentWidth(components, locale)
    
}