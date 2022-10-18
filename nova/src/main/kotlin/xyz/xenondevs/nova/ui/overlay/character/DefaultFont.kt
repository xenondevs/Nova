package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.builder.content.TextureIconContent

object DefaultFont {
    
    @Deprecated("Replaced by CharSizes", ReplaceWith("CharSizes.calculateStringLength(\"minecraft:default\", string)", "xyz.xenondevs.nova.data.resources.CharSizes"))
    fun getStringLength(string: String): Int =
        CharSizes.calculateStringLength("minecraft:default", string)
    
    @Deprecated("Replaced by CharSizes", ReplaceWith("CharSizes.getCharWidth(\"minecraft:default\", char)", "xyz.xenondevs.nova.data.resources.CharSizes"))
    fun getCharWidth(char: Char): Int =
        CharSizes.getCharWidth("minecraft:default", char)
    
    fun getVerticallyMovedText(components: Array<out BaseComponent>, distance: Int): Array<out BaseComponent> {
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