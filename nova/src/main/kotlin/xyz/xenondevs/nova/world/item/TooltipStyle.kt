package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleLayout

/**
 * Represents a custom tooltip texture.
 */
class TooltipStyle internal constructor(
    val id: Key,
    internal val makeLayout: (ResourcePackBuilder) -> TooltipStyleLayout
) {
    
    companion object {
        
        /**
         * Creates a new [TooltipStyle] with the specified [Key],
         * assuming that a tooltip style under that id already exists.
         */
        fun of(key: Key): TooltipStyle {
            return TooltipStyle(key) { throw UnsupportedOperationException() }
        }
        
    }
    
}