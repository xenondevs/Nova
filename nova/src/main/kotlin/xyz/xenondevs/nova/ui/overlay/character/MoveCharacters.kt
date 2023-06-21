package xyz.xenondevs.nova.ui.overlay.character

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap
import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.data.resources.builder.task.font.MoveCharactersContent
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import kotlin.math.abs
import kotlin.math.roundToInt

object MoveCharacters {
    
    private val componentCache = Float2ObjectOpenHashMap<Component>()
    
    private fun getMovingString(distance: Float): String {
        val start = if (distance < 0)
            ResourceLookups.MOVE_CHARACTERS_OFFSET
        else ResourceLookups.MOVE_CHARACTERS_OFFSET + MoveCharactersContent.SIZE
        
        val num = abs((distance * MoveCharactersContent.PRECISION).roundToInt())
        val buffer = StringBuffer()
        for (bit in 0 until MoveCharactersContent.SIZE)
            if (num and (1 shl bit) != 0)
                buffer.appendCodePoint(start + bit)
        
        return buffer.toString()
    }
    
    fun getMovingComponent(distance: Number): Component {
        val distFloat = distance.toFloat()
        
        var component = componentCache.get(distFloat)
        if (component != null)
            return component
        
        component = Component.text(getMovingString(distFloat))
        componentCache.put(distFloat, component)
        
        return component
    }
    
}