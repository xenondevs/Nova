package xyz.xenondevs.nova.ui.overlay.bossbar.vanilla

import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayCompound
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatchInfo
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatcher
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarPositioning
import xyz.xenondevs.nova.util.component.adventure.toPlainText

private val MARGIN_TOP = MAIN_CONFIG.entry<Int>("overlay", "bossbar", "vanilla_bars", "positioning", "margin_top")
private val MARGIN_BOTTOM = MAIN_CONFIG.entry<Int>("overlay", "bossbar", "vanilla_bars", "positioning", "margin_bottom")

internal class VanillaBossBarOverlayCompound(
    val overlay: VanillaBossBarOverlay,
    matchInfo: BarMatchInfo
) : BossBarOverlayCompound {
    
    private var _matchInfo = mutableProvider(matchInfo)
    var matchInfo by _matchInfo
    
    override val overlays = listOf(overlay)
    override var hasChanged = false
    override val positioning by combinedProvider(MARGIN_TOP, MARGIN_BOTTOM, _matchInfo)
        .map { (marginTop, marginBottom, matchInfo) ->
            BarPositioning.Dynamic(marginTop, marginBottom, matchInfo, BarMatcher.FALSE, BarMatcher.FALSE)
        }
    
    override fun getVerticalRange(locale: String): IntRange {
        return overlay.getVerticalRange(locale)
    }
    
    override fun toString(): String {
        return "VanillaBossBarOverlayCompound(text=${overlay.bar.name.toPlainText()})"
    }
    
}