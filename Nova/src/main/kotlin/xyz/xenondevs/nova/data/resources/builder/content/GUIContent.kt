package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.io.File
import javax.imageio.ImageIO

private const val ASCENT = 13

internal class GUIContent : FontContent<GUIFontChar, GUIContent.GUIData>(Resources::updateGuiDataLookup) {
    
    override fun addFromPack(pack: AssetPack) {
        pack.guisIndex?.forEach { (id, path) ->
            addFontEntry(id, path)
        }
    }
    
    override fun createFontData(id: Int, char: Char, path: String): GUIData {
        val namespace = path.substringBefore(':')
        val pathInNamespace = path.substringAfter(':')
        val file = File(ResourcePackBuilder.ASSETS_DIR, "$namespace/textures/$pathInNamespace")
        val image = ImageIO.read(file)
        return GUIData("nova:gui_$id", char, path, image.width, image.height)
    }
    
    class GUIData(font: String, char: Char, path: String, val width: Int, override val height: Int) : FontData<GUIFontChar>(font, char, path) {
        override val ascent = ASCENT
        override fun toFontInfo(): GUIFontChar = GUIFontChar(font, char, width)
    }
    
}

class GUIFontChar internal constructor(font: String, char: Char, val width: Int) : FontChar(font, char)