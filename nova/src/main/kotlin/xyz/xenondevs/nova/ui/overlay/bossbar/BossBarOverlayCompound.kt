package xyz.xenondevs.nova.ui.overlay.bossbar

import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarPositioning

/**
 * A compound of multiple [BossBarOverlays][BossBarOverlay] that should not be separated.
 */
interface BossBarOverlayCompound {
    
    val overlays: List<BossBarOverlay>
    val positioning: BarPositioning
    var hasChanged: Boolean
    
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