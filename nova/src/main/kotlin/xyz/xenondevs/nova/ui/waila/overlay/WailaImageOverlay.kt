package xyz.xenondevs.nova.ui.waila.overlay

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.builder.task.WailaBackgroundTextures
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveTo
import java.time.Duration
import kotlin.math.ceil

private const val START_TEXTURE_SIZE = 2
private const val END_TEXTURE_SIZE = 2

private const val ICON_MARGIN_LEFT = 2
private const val ICON_SIZE = 24
private const val ICON_MARGIN_RIGHT = 4

private const val TEXT_MARGIN_LEFT = 4
private const val TEXT_MARGIN_RIGHT = 2
private const val TEXT_MARGIN_VERTICAL = 8
private const val TEXT_LINE_HEIGHT = 12
private const val MIN_CONTENT_WIDTH = 20

private const val MIN_LINES = 2
private const val MAX_LINES = 10

private val overlayCache: Cache<OverlayCacheKey, OverlayData> = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .expireAfterAccess(Duration.ofMinutes(5))
    .build()

private data class OverlayCacheKey(val icon: FontChar?, val lines: Int, val longestLineLength: Float, val backgroundEnabled: Boolean)
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
    fun update(icon: FontChar?, lines: Int, longestLineLength: Float, backgroundEnabled: Boolean): Pair<Float, Float> {
        require(lines in MIN_LINES..MAX_LINES) { "Unsupported line amount: $lines" }
        
        val (component, textBeginX, textCenterX) = overlayCache.get(OverlayCacheKey(icon, lines, longestLineLength, backgroundEnabled)) {
            // left margin (2) + icon size (24) + distance between icon and text + right margin (2)
            // (margins are not counting start and end textures)
            var optimalWidth = longestLineLength + TEXT_MARGIN_RIGHT
            if (icon != null) {
                optimalWidth += ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
            } else {
                optimalWidth += TEXT_MARGIN_LEFT
            }
            
            var contentWidth = maxOf(MIN_CONTENT_WIDTH, ceil(optimalWidth).toInt())
            // An even total width keeps halfWidth integral, preventing half-pixel positions when centering the background and its contents.
            if ((START_TEXTURE_SIZE + contentWidth + END_TEXTURE_SIZE) % 2 != 0)
                contentWidth++
            val backgroundHeight = maxOf(WailaBackgroundTextures.MIN_HEIGHT, lines * TEXT_LINE_HEIGHT + TEXT_MARGIN_VERTICAL)
            val actualWidth = START_TEXTURE_SIZE + contentWidth + END_TEXTURE_SIZE
            val halfWidth = actualWidth / 2f
            
            val builder = Component.text()
            val start = if (backgroundEnabled) WailaBackgroundTextures.start(backgroundHeight) else null
            val part = if (backgroundEnabled) WailaBackgroundTextures.part(backgroundHeight) else null
            val end = if (backgroundEnabled) WailaBackgroundTextures.end(backgroundHeight) else null
            if (start != null && part != null && end != null) {
                builder
                    .move(-halfWidth)
                    .append(start.component.shadowColor(ShadowColor.none()))
                    .move(-1)
                repeat(contentWidth) {
                    builder
                        .append(part.component.shadowColor(ShadowColor.none()))
                        .move(-1)
                }
                builder.append(end.component.shadowColor(ShadowColor.none()))
            }
            
            builder
                .moveTo(-halfWidth + ICON_MARGIN_LEFT + START_TEXTURE_SIZE) // move to start icon texture
            
            if (icon != null)
                builder.append(icon.component.shadowColor(ShadowColor.none()))
            
            builder.moveTo(0)
            
            val component = builder.build()
            
            // the min x position for text to be displayed
            var textMin = -halfWidth + START_TEXTURE_SIZE
            if (icon != null) {
                textMin += ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
            } else {
                textMin += TEXT_MARGIN_LEFT
            }
            // the max x position for the text to be displayed
            val textMax = halfWidth - END_TEXTURE_SIZE - TEXT_MARGIN_RIGHT
            // the middle between textMin and textMax, which is the center point of text
            val textCenterX = (textMin + textMax) / 2
            
            return@get OverlayData(component, textMin, textCenterX)
        }
        
        this.component = component
        
        return textBeginX to textCenterX
    }
    
    override fun getWidth(locale: String): Float = 0f
    
}
