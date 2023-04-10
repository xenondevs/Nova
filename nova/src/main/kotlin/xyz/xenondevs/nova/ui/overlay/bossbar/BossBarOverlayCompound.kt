package xyz.xenondevs.nova.ui.overlay.bossbar

import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarPositioning

/**
 * A compound of multiple [BossBarOverlays][BossBarOverlay] that should not be separated.
 */
interface BossBarOverlayCompound {
    
    /**
     * The [BossBarOverlays][BossBarOverlay] of this [BossBarOverlayCompound].
     */
    val overlays: List<BossBarOverlay>
    
    /**
     * The [BarPositioning] that specifies at which position this [BossBarOverlayCompound] should be rendered.
     */
    val positioning: BarPositioning
    
    /**
     * Whether the components in the [overlays] have changed and should be re-rendered.
     */
    var hasChanged: Boolean
    
    /**
     * Gets the vertical range that the [overlays] of this [BossBarOverlayCompound] draw at.
     */
    fun getVerticalRange(locale: String): IntRange {
        var min: Int? = null
        var max: Int? = null
        
        for (overlay in overlays) {
            val range = overlay.getVerticalRange(locale)
            if (min == null || range.first < min)
                min = range.first
            if (max == null || range.last > max)
                max = range.last
        }
        
        return IntRange(min ?: 0, max ?: 0)
    }
    
}