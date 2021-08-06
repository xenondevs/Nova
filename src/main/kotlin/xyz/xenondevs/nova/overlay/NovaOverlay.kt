package xyz.xenondevs.nova.overlay

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder

enum class NovaOverlay(fontName: String, char: Char) {
    
    CRAFTING_RECIPE("gui", '0'),
    FURNACE_RECIPE("gui", '1'),
    PULVERIZER_RECIPE("gui", '2'),
    PRESS_RECIPE("gui", '3'),
    CONVERSION_RECIPE("gui", '4');
    
    val component: BaseComponent = ComponentBuilder(char.toString())
        .font("nova:$fontName")
        .color(ChatColor.WHITE)
        .create()[0]
    
    companion object {
        
        private val componentCache = HashMap<UByte, BaseComponent>()
        
        // TODO: allow more bits, add movement to the right
        private fun getMovingString(distance: UByte): String {
            val start = '\uF000'.code
            val buffer = StringBuffer()
            for (bit in 0 until 8)
                if (distance.toInt() and (1 shl bit) != 0)
                    buffer.append((start + bit).toChar())
            
            return buffer.toString()
        }
        
        fun getMovingComponent(distance: UByte): BaseComponent {
            return componentCache.getOrPut(distance) {
                ComponentBuilder(getMovingString(distance))
                    .font("nova:move")
                    .create()[0]
            }
        }
        
    }
    
}