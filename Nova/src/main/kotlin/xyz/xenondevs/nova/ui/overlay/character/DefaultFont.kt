package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.data.resources.builder.content.TextureIconContent
import xyz.xenondevs.nova.util.data.getResourceAsStream

object DefaultFont {
    
    private val charWidths = getResourceAsStream("char_widths.bin")!!.readAllBytes()
    
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
        
        val moved = components.copyOf()
        moved.forEach {
            val rawFont = it.fontRaw
            if (rawFont == null || rawFont == "default") {
                it.font = "nova:default/$distance"
            } else if (rawFont.startsWith(TextureIconContent.FONT_NAME_START)) {
                it.font = "${rawFont.substringBeforeLast('/')}/$distance"
            }
        }
        
        return moved
    }
    
}