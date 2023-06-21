package xyz.xenondevs.nova.data.resources.builder.task.font

import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups

private const val ASCENT = 13

class GuiContent internal constructor(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:gui_%s",
    true
) {
    
    @PackTask(runBefore = ["FontContent#write"])
    private fun write() {
        builder.assetPacks.forEach { pack ->
            pack.guisIndex?.forEach { (id, path) -> addEntry(id, ResourcePackBuilder.ASSETS_DIR, path, null, ASCENT) }
        }
        
        ResourceLookups.GUI_DATA_LOOKUP.set(fontCharLookup)
    }
    
}