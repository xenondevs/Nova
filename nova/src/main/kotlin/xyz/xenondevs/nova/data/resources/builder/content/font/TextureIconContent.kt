package xyz.xenondevs.nova.data.resources.builder.content.font

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
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
    
    override fun write() {
        val texturesDir = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/textures/")
        if (texturesDir.exists()) {
            exploreTextures(
                "minecraft",
                texturesDir
            )
        }
        
        super.write()
    }
    
    override fun includePack(pack: AssetPack) {
        val texturesDir = pack.texturesDir ?: return
        exploreTextures(
            pack.namespace,
            texturesDir
        )
    }
    
    private fun exploreTextures(namespace: String, dir: Path) {
        dir.walk().forEach { texture ->
            if (!texture.extension.equals("png", true))
                return@forEach
            
            val relPath = texture.relativeTo(dir).invariantSeparatorsPathString
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
