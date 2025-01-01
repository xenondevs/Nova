package xyz.xenondevs.nova.ui.waila.overlay

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.xenondevs.nova.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.util.component.adventure.append
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveTo
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

private const val WAILA_FONT = "nova:waila"

private const val START_TEXTURE_SIZE = 2
private const val END_TEXTURE_SIZE = 2
private const val PART_SIZE = 10

private const val ICON_MARGIN_LEFT = 2
private const val ICON_SIZE = 32
private const val ICON_MARGIN_RIGHT = 0

private const val TEXT_MARGIN_LEFT = 4
private const val TEXT_MARGIN_RIGHT = 4

private const val MIN_LINES = 2
private const val MAX_LINES = 10

private val overlayCache: Cache<OverlayCacheKey, OverlayData> = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build()

private data class OverlayCacheKey(val icon: FontChar?, val lines: Int, val longestLineLength: Float)
private data class OverlayData(val component: Component, val textBeginX: Float, val textCenterX: Float)

/**
 * Responsible for rendering the waila block icon and background.
 */
internal class WailaImageOverlay : BossBarOverlay {
    
    override val centerX = null
    override val offset = 0
    
    override var component: Component = Component.empty()
        private set
    
    /**
     * Updates the [WailaImageOverlay] with the given [icon] and size parameters.
     *
     * @return The x position for centering the text.
     */
    fun update(icon: FontChar?, lines: Int, longestLineLength: Float): Pair<Float, Float> {
        require(lines in MIN_LINES..MAX_LINES) { "Unsupported line amount: $lines" }
        
        val (components, textBeginX, textCenterX) = overlayCache.get(OverlayCacheKey(icon, lines, longestLineLength)) {
            // left margin (2) + icon size (32) + distance between icon and text + right margin (2)
            // (margins are not counting start and end textures)
            var optimalWidth = TEXT_MARGIN_LEFT + longestLineLength + TEXT_MARGIN_RIGHT
            if (icon != null)
                optimalWidth += ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
            
            val partAmount = ceil(optimalWidth / PART_SIZE.toDouble()).toInt()
            
            val actualWidth = START_TEXTURE_SIZE + partAmount * PART_SIZE + END_TEXTURE_SIZE
            val halfWidth = actualWidth / 2f
            
            val builder = Component.text()
                .move(-halfWidth)
                .append(getComponent(lines, 0)) // start texture
                .move(-1)
            
            repeat(partAmount) {
                builder
                    .append(getComponent(lines, 1)) // part texture
                    .move(-1)
            }
            
            builder
                .append(getComponent(lines, 2)) // end texture
                .moveTo(-halfWidth + ICON_MARGIN_LEFT + START_TEXTURE_SIZE) // move to start icon texture
            
            if (icon != null)
                builder.append(icon)
            
            builder.moveTo(0)
            
            val component = builder.build()
            
            // the min x position for text to be displayed
            var textMin = -halfWidth + TEXT_MARGIN_LEFT
            if (icon != null)
                textMin += ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
            // the max x position for the text to be displayed
            val textMax = halfWidth - TEXT_MARGIN_RIGHT
            // the middle between textMin and textMax, which is the center point of text
            val textCenterX = (textMin + textMax) / 2
            
            return@get OverlayData(component, textMin, textCenterX)
        }
        
        this.component = components
        
        return textBeginX to textCenterX
    }
    
    override fun getWidth(locale: String): Float = 0f
    
    /**
     * Gets the correct background texture char for the given [line amount][lines] and [type].
     * Valid [types][type] are 0: start, 1: part, 2: end.
     */
    private fun getChar(lines: Int, type: Int): String {
        val lineIndex = (lines - MIN_LINES).coerceAtLeast(0)
        return (0xF000 + lineIndex * 3 + type).toChar().toString()
    }
    
    /**
     * Gets the correct background texture base component for the given [line amount][lines] and [type].
     * Valid [types][type] are 0: start, 1: part, 2: end.
     */
    private fun getComponent(lines: Int, type: Int): Component {
        return Component.text(getChar(lines, type)).font(WAILA_FONT).shadowColor(ShadowColor.none())
    }
    
}