package xyz.xenondevs.nova.ui.overlay

import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.fontName

private val FORCE_UNIFORM_FONT by MAIN_CONFIG.entry<Boolean>("resource_pack", "force_uniform_font")

object MovedFonts {
    
    private val MOVED_FONT_REGEX = Regex("""([a-z0-9/._:-]*)/([\d-]*)""")
    
    /**
     * Creates a copy of the given [component] with its font changed to a vertically moved font by the given [distance].
     *
     * If the given [component] is already vertically moved, its current and new distances will only be added together if [addDistance] is true.
     *
     * Depending on the [distance] and configuration settings, the font that the component was changed to might not exist.
     *
     * @throws IllegalArgumentException If the given [component] is not a [BuildableComponent].
     */
    fun moveVertically(component: Component, distance: Int, addDistance: Boolean = false): Component {
        require(component is BuildableComponent<*, *>) { "Component must be a BuildableComponent" }
        return moveVertically(component, distance, addDistance)
    }
    
    /**
     * Creates a copy of the given [component] with its font changed to a vertically moved font by the given [distance].
     *
     * If the given [component] is already vertically moved, its current and new distances will only be added together if [addDistance] is true.
     *
     * Depending on the [distance] and configuration settings, the font that the component was changed to might not exist.
     */
    private fun moveVertically(component: BuildableComponent<*, *>, distance: Int, addDistance: Boolean = false): Component {
        fun updateFont(builder: ComponentBuilder<*, *>, previousFont: String?) {
            var font = previousFont ?: "default"
            var currentDistance = 0
            if (previousFont != null) {
                val match = MOVED_FONT_REGEX.matchEntire(previousFont)
                if (match != null) {
                    font = match.groupValues[1]
                    currentDistance = match.groupValues[2].toInt()
                }
            }
            
            if (FORCE_UNIFORM_FONT && (font == "default" || font == "minecraft:default"))
                font = "uniform"
            
            val newDistance = if (addDistance) currentDistance + distance else distance
            if (newDistance != 0) {
                builder.font("$font/${newDistance}")
            } else {
                builder.font(font)
            }
        }
        
        val builder = component.toBuilder()
        updateFont(builder, component.fontName())
        builder.mapChildrenDeep {
            val childBuilder = it.toBuilder()
            updateFont(childBuilder, it.fontName())
            childBuilder.build()
        }
        
        return builder.build()
    }
    
}