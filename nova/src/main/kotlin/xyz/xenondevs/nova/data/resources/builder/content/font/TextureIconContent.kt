package xyz.xenondevs.nova.data.resources.builder.content.font

import net.lingala.zip4j.model.FileHeader
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.listFileHeaders
import xyz.xenondevs.nova.util.data.path
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private const val HEIGHT = 16
private const val ASCENT = 12

internal class TextureIconContent(
    movedFontContent: MovedFontContent
) : FontContent(
    "nova:texture_icons_%s",
    Resources::updateTextureIconLookup,
    movedFontContent
) {
    
    override val stage = ResourcePackBuilder.BuildingStage.PRE_WORLD
    
    companion object {
        const val FONT_NAME_START = "nova:texture_icons"
    }
    
    override fun write() {
        val texturesDir = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/textures/")
        if (texturesDir.exists()) {
            exploreTextures(
                "minecraft",
                texturesDir,
                texturesDir.walk()
                    .filter { !it.isDirectory() && it.extension == "png" }
            )
        }
        
        super.write()
    }
    
    override fun includePack(pack: AssetPack) {
        val texturesDir = pack.texturesDir ?: return
        exploreTextures(
            pack.namespace,
            texturesDir.path,
            pack.zip.listFileHeaders(texturesDir)
                .filterNot(FileHeader::isDirectory)
                .map(FileHeader::path)
        )
    }
    
    private fun exploreTextures(namespace: String, basePath: Path, textures: Sequence<Path>) {
        textures.forEach { texture ->
            val relPath = texture.relativeTo(basePath).invariantSeparatorsPathString
            if ((!relPath.startsWith("block/") && !relPath.startsWith("item/")) || !relPath.endsWith(".png"))
                return@forEach
            
            addFontEntry(
                "${namespace}:${relPath.substringBeforeLast('.')}",
                ResourcePath(namespace, relPath),
                HEIGHT,
                ASCENT
            )
        }
    }
    
}
