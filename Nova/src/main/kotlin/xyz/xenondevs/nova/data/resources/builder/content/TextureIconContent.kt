package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.io.File

private const val SIZE = 16
private const val ASCENT = 12

internal class TextureIconContent : FontContent<FontChar, TextureIconContent.TextureFontData>(
    Resources::updateTextureIconLookup, true
) {
    
    init {
        val texturesDir = File(ResourcePackBuilder.MCASSETS_DIR, "assets/minecraft/textures/")
        if (texturesDir.exists())
            exploreTexturesDir("minecraft", texturesDir)
    }
    
    override fun addFromPack(pack: AssetPack) {
        pack.texturesDir?.let { exploreTexturesDir(pack.namespace, it) }
    }
    
    private fun exploreTexturesDir(namespace: String, texturesDir: File) {
        val itemDir = File(texturesDir, "item/").takeIf(File::exists) ?: return
        itemDir.walkTopDown().forEach { file ->
            if (file.isDirectory || !file.extension.equals("png", true))
                return@forEach
            
            val relPath = file.relativeTo(texturesDir).path.replace('\\', '/')
            addFontEntry(
                "${namespace}:${relPath.substringBeforeLast('.')}",
                "${namespace}:$relPath"
            )
        }
    }
    
    override fun createFontData(id: Int, char: Char, path: String): TextureFontData =
        TextureFontData("${FONT_NAME_START}_$id", char, path)
    
    class TextureFontData(font: String, char: Char, path: String) : FontData<FontChar>(font, char, path) {
        override val height = SIZE
        override val ascent = ASCENT
        override fun toFontInfo() = FontChar("$font/0", char)
    }
    
    companion object {
        const val FONT_NAME_START = "nova:texture_icons_"
    }
    
}
