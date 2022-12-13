package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.io.File

private const val HEIGHT = 16
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
        (File(texturesDir, "block").walkTopDown() + File(texturesDir, "item").walkTopDown())
            .forEach { file ->
                if (file.isDirectory || !file.extension.equals("png", true))
                    return@forEach
                
                val relPath = file.relativeTo(texturesDir).path.replace('\\', '/')
                addFontEntry(
                    "${namespace}:${relPath.substringBeforeLast('.')}",
                    ResourcePath(namespace, relPath)
                )
            }
    }
    
    override fun createFontData(id: Int, char: Char, path: ResourcePath): TextureFontData =
        TextureFontData("${FONT_NAME_START}_$id", char, path, getWidth(HEIGHT, path))
    
    class TextureFontData(font: String, char: Char, path: ResourcePath, width: Int) : FontData<FontChar>(font, char, path, width) {
        override val height = HEIGHT
        override val ascent = ASCENT
        override fun toFontInfo() = FontChar("$font/0", char, width)
    }
    
    companion object {
        const val FONT_NAME_START = "nova:texture_icons"
    }
    
}
