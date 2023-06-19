package xyz.xenondevs.nova.ui.overlay.character

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.util.component.adventure.font
import kotlin.math.abs

object MoveCharacters {
    
    private val componentCache = HashMap<Int, Component>()
    
    private fun getMovingString(distance: Int): String {
        val start = if (distance < 0) '\uF000'.code else '\uF100'.code
        val num = abs(distance)
        val buffer = StringBuffer()
        for (bit in 0 until 31)
            if (num and (1 shl bit) != 0)
                buffer.append((start + bit).toChar())
        
        return buffer.toString()
    }
    
    fun getMovingComponent(distance: Number): Component {
        val distance = distance.toInt() // TODO
        
        return componentCache.getOrPut(distance) {
            Component.text()
                .content(getMovingString(distance))
                .font("nova:move")
                .build()
        }
    }
    
}