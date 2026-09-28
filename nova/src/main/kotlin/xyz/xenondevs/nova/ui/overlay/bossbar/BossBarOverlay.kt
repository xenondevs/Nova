package xyz.xenondevs.nova.ui.overlay.bossbar

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.CharSizes

interface BossBarOverlay {
    
    /**
     * The vertical offset of this overlay, relative to the offset of the [BossBarOverlayCompound]. Gui-scale affected.
     */
    val offset: Int
    
    /**
     * At which x-coordinate the [Component] should be centered at. Gui-scale affected.
     * Can be null if they shouldn't be centered.
     */
    val centerX: Float?
    
    /**
     * At which x-coordinate the visual left edge of the [Component] should be placed. Gui-scale affected.
     * Can be null if it should not be left-aligned.
     */
    val leftX: Float?
        get() = null
    
    /**
     * The [Component] of this [BossBarOverlay].
     */
    val component: Component
    
    /**
     * Gets the width of the [Component] in pixels.
     */
    fun getWidth(locale: String): Float =
        CharSizes.calculateComponentWidth(component, locale)
    
    /**
     * Gets the vertical range of this overlay in pixels, relative from the [offset] position.
     */
    fun getVerticalRange(locale: String): IntRange {
        val componentRange = CharSizes.calculateComponentSize(component, locale, true).yRange
        return IntRange(offset + componentRange.start.toInt(), offset + componentRange.endInclusive.toInt())
    }
    
}