package xyz.xenondevs.nova.ui.overlay.character

import net.md_5.bungee.api.chat.BaseComponent

object MovedFonts {
    
    private val MOVED_FONT_REGEX = Regex("""([a-z0-9/._:-]*)/([\d-]*)""")
    
    /**
     * Changes the selected fonts in the given [components] to vertically moved fonts by the given [distance].
     * 
     * If the given [components] are already vertically moved, their current and new distances will only be added together if [addDistance] is true.
     *
     * Depending on the [distance] and configuration settings, the fonts that the components were changed to might not exist.
     */
    fun moveVertically(components: Array<out BaseComponent>, distance: Int, addDistance: Boolean = false): Array<out BaseComponent> {
        components.forEach {
            var font = it.fontRaw ?: "default"
            var currentDistance = 0
            val match = MOVED_FONT_REGEX.matchEntire(font)
            if (match != null) {
                font = match.groupValues[1]
                if (addDistance) {
                    currentDistance = match.groupValues[2].toInt()
                }
            }
            it.font = "$font/${currentDistance + distance}"
        }
        
        return components
    }
    
}