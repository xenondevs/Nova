package xyz.xenondevs.nova.data.resources.builder.content.font

import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.CharSizeCalculator
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.BuildingStage
import xyz.xenondevs.nova.data.resources.builder.content.PackContentType

private const val ASCENT = 13

class GuiContent private constructor() : FontContent(
    "nova:gui_%s",
    ResourceGeneration::updateGuiDataLookup
) {
    
    companion object : PackContentType<GuiContent> {
        override val stage = BuildingStage.PRE_WORLD // writes to Resources
        override val runBefore = setOf(CharSizeCalculator)
        override fun create(builder: ResourcePackBuilder) = GuiContent()
    }
    
    override val movedFontContent = null
    
    override fun includePack(pack: AssetPack) {
        pack.guisIndex?.forEach { (id, path) -> addFontEntry(id, path, null, ASCENT) }
    }
    
}