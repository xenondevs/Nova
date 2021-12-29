package xyz.xenondevs.nova.ui.overlay

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import kotlin.math.abs

enum class CustomCharacters(fontName: String, char: Char) {
    
    CRAFTING_RECIPE("gui", '0'),
    FURNACE_RECIPE("gui", '1'),
    PULVERIZER_RECIPE("gui", '2'),
    PRESS_RECIPE("gui", '3'),
    CONVERSION_RECIPE("gui", '4'),
    EMPTY_GUI("gui", '5'),
    SEARCH("gui", '6'),
    CREATIVE_0("gui", 'a'),
    CREATIVE_1("gui", 'b'),
    CREATIVE_2("gui", 'c'),
    CREATIVE_3("gui", 'd'),
    CREATIVE_4("gui", 'e');
    
    val component: BaseComponent = ComponentBuilder(char.toString())
        .font("nova:$fontName")
        .color(ChatColor.WHITE)
        .create()[0]
    
    companion object {
        
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
                ComponentBuilder(getMovingString(distance))
                    .font("nova:move")
                    .create()[0]
            }
        }
        
        fun getStringLength(string: String): Int {
            var size = 0
            string.toCharArray().forEach { size += getCharSize(it) }
            return size
        }
        
        fun getCharSize(char: Char): Int =
            when (char) {
                'k', 'f' -> 4
                't', 'I', ' ' -> 3
                'l' -> 2
                'i' -> 1
                else -> 5
            } + 1
        
    }
    
}