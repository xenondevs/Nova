package xyz.xenondevs.nova.ui.overlay

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.builder.task.MoveCharactersTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.novaKey
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Utility for generating horizontally moving characters (positive and negative spaces).
 */
object MoveCharacters {
    
    private val MOVE_FONT_KEY = novaKey("move")
    
    internal fun getMovingString(distance: Float): String {
        val start = if (distance < 0)
            ResourceLookups.moveCharactersOffset
        else ResourceLookups.moveCharactersOffset + MoveCharactersTask.SIZE
        
        val num = abs((distance * MoveCharactersTask.PRECISION).roundToInt())
        val buffer = StringBuffer()
        for (bit in 0..<MoveCharactersTask.SIZE) {
            if (num and (1 shl bit) != 0)
                buffer.appendCodePoint(start + bit)
        }
        
        return buffer.toString()
    }
    
    /**
     * Gets a [Component] that horizontally moves the cursor by [distance] gui-affected pixels.
     */
    fun getMovingComponent(distance: Number): Component =
        Component.text(getMovingString(distance.toFloat())).font(MOVE_FONT_KEY)
    
}