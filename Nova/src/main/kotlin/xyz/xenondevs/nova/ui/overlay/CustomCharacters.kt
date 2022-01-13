package xyz.xenondevs.nova.ui.overlay

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import kotlin.math.abs

enum class GUITexture(private val char: Char, private val width: Int) {
    
    EMPTY_GUI('\uF000', 176),
    MECHANICAL_BREWING_STAND('\uF200', 176),
    CONFIGURE_POTION('\uF201', 176),
    PICK_COLOR('\uF202', 176);
    
    fun getTitle(translate: String): Array<BaseComponent> {
        return getTitle(TranslatableComponent(translate))
    }
    
    fun getTitle(title: TranslatableComponent): Array<BaseComponent> {
        return ComponentBuilder()
            .append(CustomCharacters.getMovingComponent(-8)) // move to side to place overlay
            .append(char.toString())
            .font("nova:gui")
            .color(ChatColor.WHITE)
            .append(CustomCharacters.getMovingComponent(-width + 7)) // move back to start
            .append(title)
            .font("default")
            .color(ChatColor.DARK_GRAY)
            .create()
        
    }
    
}

enum class CustomCharacters(fontName: String, char: Char) {
    
    // TODO: move these to GUITexture
    EMPTY_GUI("gui", '\uF000'),
    CRAFTING_RECIPE("gui", '\uF001'),
    FURNACE_RECIPE("gui", '\uF002'),
    CONVERSION_RECIPE("gui", '\uF003'),
    SMITHING_TABLE("gui", '\uF004'),
    PULVERIZER_RECIPE("gui", '\uF005'),
    PRESS_RECIPE("gui", '\uF006'),
    FLUID_INFUSER("gui", '\uF007'),
    STAR_COLLECTOR("gui", '\uF008'),
    COBBLESTONE_GENERATOR("gui", '\uF009'),
    FREEZER("gui", '\uF00A'),
    SEARCH("gui", '\uF00B'),
    CREATIVE_0("gui", '\uF100'),
    CREATIVE_1("gui", '\uF101'),
    CREATIVE_2("gui", '\uF102'),
    CREATIVE_3("gui", '\uF103'),
    CREATIVE_4("gui", '\uF104');
    
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