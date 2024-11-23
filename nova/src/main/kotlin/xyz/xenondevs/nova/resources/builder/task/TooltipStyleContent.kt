package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder

class TooltipStyleContent internal constructor(
    private val builder: ResourcePackBuilder
) : PackTaskHolder {
    
    @PackTask
    fun write() {
        for (style in NovaRegistries.TOOLTIP_STYLE) {
            val layout = style.makeLayout.invoke(builder)
            
            if (layout.backgroundMeta != null)
                builder.writeMeta(layout.backgroundPath, layout.backgroundMeta)
            if (layout.frameMeta != null)
                builder.writeMeta(layout.framePath, layout.frameMeta)
        }
        
    }
    
}