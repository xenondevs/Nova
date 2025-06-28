package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleBuilder
import xyz.xenondevs.nova.world.item.TooltipStyle

@Suppress("DEPRECATION")
@Deprecated(REGISTRIES_DEPRECATION)
interface TooltipStyleRegistry : AddonGetter {
    
    /**
     * Registers a new [TooltipStyle] with the specified [name] and [meta].
     *
     * The tooltip textures are expected to be located under `textures/gui/sprites/tooltip/<name>_background.png` and
     * `textures/gui/sprites/tooltip/<name>_frame.png`. Their mcmeta can be configured via [meta].
     */
    @Deprecated(REGISTRIES_DEPRECATION)
    fun tooltipStyle(name: String, meta: TooltipStyleBuilder.() -> Unit): TooltipStyle =
        addon.tooltipStyle(name, meta)
    
}