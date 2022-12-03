package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.util.data.readImage

private const val ASCENT = 13

internal class GUIContent : FontContent<FontChar, GUIContent.GUIData>(Resources::updateGuiDataLookup) {
    
    override fun includePack(pack: AssetPack) {
        pack.guisIndex?.forEach { (id, path) -> addFontEntry(id, path) }
    }
    
    override fun createFontData(id: Int, char: Char, path: ResourcePath): GUIData {
        val image = getFile(path).readImage()
        return GUIData("nova:gui_$id", char, path, image.width, image.height)
    }
    
    class GUIData(font: String, char: Char, path: ResourcePath, width: Int, override val height: Int) : FontData<FontChar>(font, char, path, width) {
        override val ascent = ASCENT
        override fun toFontInfo(): FontChar = FontChar(font, char, width)
    }
    
}