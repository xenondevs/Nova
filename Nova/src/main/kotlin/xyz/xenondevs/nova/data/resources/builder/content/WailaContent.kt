package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.io.File

private const val SIZE = 32
private const val ASCENT = -4

internal class WailaContent : FontContent<FontChar, WailaContent.WailaIconData>(Resources::updateWailaDataLookup) {
    
    override fun addFromPack(pack: AssetPack) {
        if (!DEFAULT_CONFIG.getBoolean("waila.enabled")) {
            val texturesDir = File(ResourcePackBuilder.ASSETS_DIR, "${pack.namespace}/textures/waila/")
            texturesDir.deleteRecursively()
            return
        }
        
        val wailaDir = pack.texturesDir?.let { File(it, "waila/") }
        if (wailaDir == null || !wailaDir.exists())
            return
        
        wailaDir.walkTopDown().forEach { file ->
            if (file.isDirectory || !file.extension.equals("png", true))
                return@forEach
            
            val idNamespace = pack.namespace.takeUnless { it == "nova" } ?: "minecraft" // all textures form "nova" asset pack are for minecraft blocks
            val id = "$idNamespace:${file.nameWithoutExtension}"
            val path = "${pack.namespace}:waila/${file.name}"
            
            addFontEntry(id, path)
        }
    }
    
    override fun createFontData(id: Int, char: Char, path: String): WailaIconData =
        WailaIconData("nova:waila_textures_$id", char, path)
    
    class WailaIconData(font: String, char: Char, path: String) : FontData<FontChar>(font, char, path) {
        override val height = SIZE
        override val ascent = ASCENT
        override fun toFontInfo(): FontChar = FontChar(font, char)
    }
    
}