package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.util.data.formatWithTemplate
import xyz.xenondevs.nova.util.data.getResourceAsStream

object DefaultFont {
    
    private val charWidths = getResourceAsStream("char_widths.bin")!!.readAllBytes()
    private val vertMoveTemplates = HashMap<Int, BaseComponent>()
    
    fun getStringLength(string: String): Int {
        var size = 0
        string.toCharArray().forEach { size += getCharWidth(it) }
        return size
    }
    
    fun getCharWidth(char: Char): Int =
        charWidths[char.code].toInt()
    
    fun getVerticallyMovedText(components: Array<BaseComponent>, distance: Int): Array<BaseComponent> {
        // Due to Minecraft's inefficient font loading, too many fonts will cause the client to crash, even though
        // the same bitmap files are used.
        // For that reason, only 20 vertical movement fonts are included for now.
        require(distance in -20..0) { "No font for vertical distance $distance available" }
        
        return components.formatWithTemplate(vertMoveTemplates.getOrPut(distance) {
            ComponentBuilder("")
                .font("nova:default/$distance")
                .create()[0]
        })
    }
    
}