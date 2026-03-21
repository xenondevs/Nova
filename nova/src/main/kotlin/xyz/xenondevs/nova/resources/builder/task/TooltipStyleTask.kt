package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleLayout
import xyz.xenondevs.nova.world.item.TooltipStyle

/**
 * Generates tooltip style assets.
 */
class TooltipStyleTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    
    override suspend fun run() {
        for ((_, makeLayout) in requests) {
            val layout = makeLayout.invoke(builder)
            
            if (layout.backgroundMeta != null)
                builder.writeMeta(layout.backgroundPath, layout.backgroundMeta)
            if (layout.frameMeta != null)
                builder.writeMeta(layout.framePath, layout.frameMeta)
        }
    }
    
    internal companion object {
        
        private val requests = HashMap<RegistryEntry.Nova<TooltipStyle>, (ResourcePackBuilder) -> TooltipStyleLayout>()
        
        /**
         * Requests the generation of tooltip style assets for the given [style] using [makeLayout].
         */
        fun request(
            style: RegistryEntry.Nova<TooltipStyle>,
            makeLayout: (ResourcePackBuilder) -> TooltipStyleLayout
        ) {
            requests[style] = makeLayout
        }
        
    }
    
}