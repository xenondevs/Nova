package xyz.xenondevs.nova.ui.waila.overlay

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.data.resources.builder.content.FontChar
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.util.data.MovingComponentBuilder
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

private const val WAILA_FONT = "nova:waila"

private const val START_TEXTURE_SIZE = 2
private const val END_TEXTURE_SIZE = 2
private const val PART_SIZE = 10

private const val ICON_MARGIN_LEFT = 2
private const val ICON_SIZE = 32
private const val ICON_MARGIN_RIGHT = 2

private const val TEXT_MARGIN_RIGHT = 4

private const val MIN_LINES = 2
private const val MAX_LINES = 10

private val overlayCache: Cache<OverlayCacheKey, OverlayData> = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build()

private data class OverlayCacheKey(val icon: FontChar?, val lines: Int, val longestLineLength: Int)
private typealias OverlayData = Triple<Array<BaseComponent>, Int, Int>

internal class WailaImageOverlay : BossBarOverlay() {
    
    override val barLevel = 0
    override val centerX = null
    override val width = 0
    
    override var endY = 0
        private set
    
    override var components: Array<BaseComponent> = ComponentBuilder("").create()
        private set
    
    /**
     * Updates the [WailaImageOverlay] with the given [icon] and size parameters.
     *
     * @return The x position for centering the text.
     */
    fun update(icon: FontChar?, lines: Int, longestLineLength: Int): Pair<Int, Int> {
        require(lines in MIN_LINES..MAX_LINES) { "Unsupported line amount: $lines" }
        
        val (components, textBeginX, textCenterX) = overlayCache.get(OverlayCacheKey(icon, lines, longestLineLength)) {
            // left margin (2) + icon size (32) + distance between icon and text + right margin (2)
            // (margins are not counting start and end textures)
            val optimalWidth = ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT + longestLineLength + TEXT_MARGIN_RIGHT
            
            val partAmount = ceil(optimalWidth / PART_SIZE.toDouble()).toInt()
            
            val actualWidth = START_TEXTURE_SIZE + partAmount * PART_SIZE + END_TEXTURE_SIZE
            val halfWidth = actualWidth / 2
            
            val builder = MovingComponentBuilder()
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
            
            val components = builder.create()
            
            // the min x position for text to be displayed
            val textMin = -halfWidth + START_TEXTURE_SIZE + ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
            // returns the middle between textMin and the end of the box, which is the center point of text
            val textCenterX = textMin + (halfWidth - textMin - TEXT_MARGIN_RIGHT) / 2
            
            return@get Triple(components, textMin, textCenterX)
        }
        
        this.components = components
        this.endY = -getTextureHeight(lines) + 1
        
        return textBeginX to textCenterX
    }
    
    /**
     * Gets the height of the background texture for the given amount of lines.
     */
    private fun getTextureHeight(lines: Int): Int =
        when {
            lines < 0 -> throw UnsupportedOperationException()
            lines in 0..2 -> 40
            else -> 8 + lines * 12
        }
    
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
    private fun getComponent(lines: Int, type: Int): BaseComponent {
        return TextComponent(getChar(lines, type)).apply {
            font = WAILA_FONT
            color = ChatColor.WHITE
        }
    }
    
}