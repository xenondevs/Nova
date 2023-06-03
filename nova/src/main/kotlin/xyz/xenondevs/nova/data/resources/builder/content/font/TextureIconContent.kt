package xyz.xenondevs.nova.data.resources.builder.content.font

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.CharSizeCalculator
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.BuildingStage
import xyz.xenondevs.nova.data.resources.builder.content.PackContentType
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private const val HEIGHT = 16
private const val ASCENT = 12

class TextureIconContent private constructor(
    builder: ResourcePackBuilder
) : FontContent(
    "nova:texture_icons_%s",
    Resources::updateTextureIconLookup
) {
    
    companion object : PackContentType<TextureIconContent> {
        override val stage = BuildingStage.PRE_WORLD // writes to Resources
        override val runBefore = setOf(CharSizeCalculator)
        override fun create(builder: ResourcePackBuilder) = TextureIconContent(builder)
    }
    
    override val movedFontContent by lazy { builder.getContent(MovedFontContent) }
    
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
