package xyz.xenondevs.nova.ui.waila.overlay

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.data.resources.builder.WailaIconData
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import kotlin.math.ceil

private const val WAILA_FONT = "nova:waila"

private const val START_TEXTURE_SIZE = 2
private const val END_TEXTURE_SIZE = 2
private const val PART_SIZE = 10

private const val ICON_MARGIN_LEFT = 2
private const val ICON_SIZE = 32
private const val ICON_MARGIN_RIGHT = 2

private const val TEXT_MARGIN_RIGHT = 2

private const val MIN_LINES = 2
private const val MAX_LINES = 10

internal class WailaImageOverlay : BossBarOverlay() {
    
    override val barLevel = 0
    override val centerX = null
    override val width = 0
    
    override var components: Array<BaseComponent> = ComponentBuilder("").create()
        private set
    
    /**
     * Updates the [WailaImageOverlay] with the given [icon] and size parameters.
     *
     * @return The x position for centering the text.
     */
    fun update(icon: WailaIconData, lines: Int, longestLineLength: Int): Int {
        require(lines in MIN_LINES..MAX_LINES) { "Unsupported line amount: $lines" }
        
        // left margin (2) + icon size (32) + distance between icon and text + right margin (2)
        // (margins are not counting start and end textures)
        val optimalWidth = ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT + longestLineLength + TEXT_MARGIN_RIGHT
        
        val partAmount = ceil(optimalWidth / PART_SIZE.toDouble()).toInt()
        
        val actualWidth = START_TEXTURE_SIZE + partAmount * PART_SIZE + END_TEXTURE_SIZE
        val halfWidth = actualWidth / 2
        
        val builder = ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(-halfWidth))
            .append(getComponent(lines, 0)) // start texture
            .append(MoveCharacters.getMovingComponent(-1)) // move one back
        
        repeat(partAmount) {
            builder
                .append(getComponent(lines, 1)) // part texture
                .append(MoveCharacters.getMovingComponent(-1)) // move one back
        }
        
        components = builder
            .append(getComponent(lines, 2)) // end texture
            .append(MoveCharacters.getMovingComponent(-actualWidth - 1 + ICON_MARGIN_LEFT + START_TEXTURE_SIZE)) // move to start icon texture
            .append(icon.char.toString()).font(icon.font) // icon texture
            .append(MoveCharacters.getMovingComponent(-1 - ICON_SIZE - ICON_MARGIN_LEFT - START_TEXTURE_SIZE + halfWidth)) // move back to the middle
            .create()
        
        // the min x position for text to be displayed
        val textMin = -halfWidth + START_TEXTURE_SIZE + ICON_MARGIN_LEFT + ICON_SIZE + ICON_MARGIN_RIGHT
        // returns the middle between textMin and the end of the box, which is the center point of text
        return textMin + (halfWidth - textMin) / 2
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