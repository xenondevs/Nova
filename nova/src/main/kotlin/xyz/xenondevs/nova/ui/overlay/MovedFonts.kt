package xyz.xenondevs.nova.ui.overlay

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import kotlin.math.roundToInt

object MovedFonts {
    
    private val MOVED_FONT_REGEX = Regex("""(.+)/(-?\d+)""")
    
    /**
     * Creates a copy of [component] that is moved vertically by [distance].
     *
     * If [component] already uses a moved font, [addDistance] controls whether [distance] is added to its current
     * movement or replaces it.
     *
     * Depending on the [distance] and configuration settings, the font that the component was changed to might not exist.
     */
    fun moveVertically(component: Component, distance: Int, addDistance: Boolean = false): Component =
        moveComponent(component, Style.empty(), 0, distance, addDistance)
    
    /**
     * Applies the requested moved font to [builder].
     *
     * Returns the horizontal correction needed for italic text.
     */
    private fun applyMovedFont(
        builder: ComponentBuilder<*, *>,
        previousFont: String?,
        distance: Int,
        addDistance: Boolean
    ): Int? {
        if (previousFont == "nova:move")
            return null
        
        var font = previousFont ?: "default"
        var currentDistance = 0
        if (previousFont != null) {
            val match = MOVED_FONT_REGEX.matchEntire(previousFont)
            if (match != null) {
                font = match.groupValues[1]
                currentDistance = match.groupValues[2].toInt()
            }
        }
        
        val newDistance = if (addDistance) currentDistance + distance else distance
        val newFont = ResourcePath.of(ResourceType.Font, "$font/$newDistance")
        if (newFont in ResourceLookups.movedFonts)
            builder.font(if (newFont in ResourceLookups.movedFonts) newFont else Key.key(font))
        
        return (newDistance / 4f).roundToInt() - (currentDistance / 4f).roundToInt()
    }
    
    /**
     * Moves [component] and everything nested inside it.
     *
     * [parentStyle] is the inherited style.
     * [activeItalicCompensation] is the amount of pre-existing italic correction.
     */
    private fun moveComponent(
        component: Component,
        parentStyle: Style,
        activeItalicCompensation: Int,
        distance: Int,
        addDistance: Boolean
    ): Component {
        val effectiveStyle = parentStyle.merge(component.style())
        val builder = component.toBuilder()
        val fontCompensation = applyMovedFont(builder, effectiveStyle.font()?.asString(), distance, addDistance)
        val italicCompensation = when {
            fontCompensation == null -> activeItalicCompensation
            effectiveStyle.hasDecoration(TextDecoration.ITALIC) -> fontCompensation
            else -> 0
        }
        
        var moved = builder.build()
        if (moved is TranslatableComponent) {
            moved = moved.arguments(moved.arguments().map { argument ->
                val value = argument.value()
                if (value is Component) {
                    TranslationArgument.component(
                        moveComponent(value, effectiveStyle, italicCompensation, distance, addDistance)
                    )
                } else {
                    argument
                }
            })
        }
        moved = moved.children(component.children().map {
            moveComponent(it, effectiveStyle, italicCompensation, distance, addDistance)
        })
        
        val compensationDelta = italicCompensation - activeItalicCompensation
        if (compensationDelta == 0)
            return moved
        
        return Component.text()
            .append(MoveCharacters.getMovingComponent(compensationDelta))
            .append(moved)
            .append(MoveCharacters.getMovingComponent(-compensationDelta))
            .build()
    }
    
}