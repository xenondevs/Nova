package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleBuilder
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.TooltipStyle

interface TooltipStyleRegistry : AddonGetter {
    
    /**
     * Registers a new [TooltipStyle] with the specified [name] and [meta].
     *
     * The tooltip textures are expected to be located under `textures/gui/sprites/tooltip/<name>_background.png` and
     * `textures/gui/sprites/tooltip/<name>_frame.png`. Their mcmeta can be configured via [meta].
     */
    fun tooltipStyle(name: String, meta: TooltipStyleBuilder.() -> Unit): TooltipStyle {
        val id = Key(addon, name)
        val style = TooltipStyle(id) { TooltipStyleBuilder(id, it).apply(meta).build() }
        NovaRegistries.TOOLTIP_STYLE[id] = style
        return style
    }
    
    // TODO: animatedTooltipStyle
    
}