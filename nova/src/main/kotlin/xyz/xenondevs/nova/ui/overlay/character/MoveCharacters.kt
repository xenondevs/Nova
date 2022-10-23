package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import kotlin.math.abs

object MoveCharacters {
    
    private val FORMATTING_TEMPLATE = ComponentBuilder("")
        .font("nova:move")
        .bold(false)
        .obfuscated(false)
        .create()[0]
    
    private val componentCache = HashMap<Int, BaseComponent>()
    
    private fun getMovingString(distance: Int): String {
        val start = if (distance < 0) '\uF000'.code else '\uF100'.code
        val num = abs(distance)
        val buffer = StringBuffer()
        for (bit in 0 until 31)
            if (num and (1 shl bit) != 0)
                buffer.append((start + bit).toChar())
        
        return buffer.toString()
    }
    
    fun getMovingComponent(distance: Int): BaseComponent {
        return componentCache.getOrPut(distance) {
            val component = TextComponent(getMovingString(distance))
            component.copyFormatting(FORMATTING_TEMPLATE)
            return@getOrPut component
        }.duplicate()
    }
    
}