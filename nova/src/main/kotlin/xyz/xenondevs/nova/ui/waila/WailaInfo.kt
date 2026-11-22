package xyz.xenondevs.nova.ui.waila

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.builder.task.FontChar

/**
 * The content of the WAILA overlay: an [icon] (or none if `null`) followed by 1 to 10 text [lines].
 *
 * @see WailaInfoProvider
 */
data class WailaInfo(val icon: FontChar?, val lines: List<WailaLine>) {
    
    init {
        require(lines.size in 1..10) { "WAILA info must have between 1 and 10 lines, but has ${lines.size}" }
        val firstAlignment = lines.first().alignment
        require(firstAlignment != WailaLine.Alignment.FIRST_LINE && firstAlignment != WailaLine.Alignment.PREVIOUS_LINE) {
            "The first line of WAILA info can't be aligned to $firstAlignment"
        }
    }
    
}

/**
 * A line of [text] in the WAILA overlay, positioned according to [alignment].
 */
data class WailaLine(val text: Component, val alignment: Alignment) {
    
    /**
     * The horizontal alignment of a [WailaLine].
     */
    enum class Alignment {
        
        /**
         * Aligned to the left edge of the text area.
         */
        LEFT,
        
        /**
         * Centered in the text area.
         */
        CENTERED,
        
        /**
         * Starts at the same x position as the first line. Not allowed for the first line.
         */
        FIRST_LINE,
        
        /**
         * Starts at the same x position as the previous line. Not allowed for the first line.
         */
        PREVIOUS_LINE
        
    }
    
}