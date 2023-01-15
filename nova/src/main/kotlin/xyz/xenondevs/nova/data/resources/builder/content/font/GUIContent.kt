package xyz.xenondevs.nova.data.resources.builder.content.font

import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder

private const val ASCENT = 13

internal class GUIContent : FontContent(
    "nova:gui_%s",
    Resources::updateGuiDataLookup
) {
    
    override val stage = ResourcePackBuilder.BuildingStage.PRE_WORLD
    
    override fun includePack(pack: AssetPack) {
        pack.guisIndex?.forEach { (id, path) -> addFontEntry(id, path, null, ASCENT) }
    }
    
}