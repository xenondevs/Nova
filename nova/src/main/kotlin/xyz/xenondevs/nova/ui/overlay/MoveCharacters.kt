package xyz.xenondevs.nova.ui.overlay

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.builder.task.font.MoveCharactersContent
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import kotlin.math.abs
import kotlin.math.roundToInt

object MoveCharacters {
    
    internal fun getMovingString(distance: Number): String =
        getMovingString(distance.toFloat())
    
    internal fun getMovingString(distance: Float): String {
        val start = if (distance < 0)
            ResourceLookups.MOVE_CHARACTERS_OFFSET
        else ResourceLookups.MOVE_CHARACTERS_OFFSET + MoveCharactersContent.SIZE
        
        val num = abs((distance * MoveCharactersContent.PRECISION).roundToInt())
        val buffer = StringBuffer()
        for (bit in 0..<MoveCharactersContent.SIZE) {
            if (num and (1 shl bit) != 0)
                buffer.appendCodePoint(start + bit)
        }
        
        return buffer.toString()
    }
    
    fun getMovingComponent(distance: Number): Component =
        Component.text(getMovingString(distance.toFloat()))
    
}