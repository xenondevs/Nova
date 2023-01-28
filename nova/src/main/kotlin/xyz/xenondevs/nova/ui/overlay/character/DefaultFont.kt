package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.data.resources.CharSizes

object DefaultFont {
    
    @Deprecated("Replaced by CharSizes", ReplaceWith("CharSizes.calculateStringLength(\"minecraft:default\", string)", "xyz.xenondevs.nova.data.resources.CharSizes"))
    fun getStringLength(string: String): Int =
        CharSizes.calculateStringWidth("minecraft:default", string)
    
    @Deprecated("Replaced by CharSizes", ReplaceWith("CharSizes.getCharWidth(\"minecraft:default\", char)", "xyz.xenondevs.nova.data.resources.CharSizes"))
    fun getCharWidth(char: Char): Int =
        CharSizes.getCharWidth("minecraft:default", char)
    
    @Deprecated("Replaced by MovedFonts", ReplaceWith("MovedFonts.moveVertically(components, distance)"))
    fun getVerticallyMovedText(components: Array<out BaseComponent>, distance: Int): Array<out BaseComponent>  =
        MovedFonts.moveVertically(components, distance)
    
}