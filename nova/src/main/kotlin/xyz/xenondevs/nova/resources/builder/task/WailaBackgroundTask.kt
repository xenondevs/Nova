package xyz.xenondevs.nova.resources.builder.task

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.waila.WailaManager
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object WailaBackgroundTextures {
    
    const val MIN_HEIGHT = 20
    const val MAX_HEIGHT = 128
    
    fun start(height: Int): FontChar? = get(height, 0)
    fun part(height: Int): FontChar? = get(height, 1)
    fun end(height: Int): FontChar? = get(height, 2)
    
    private fun get(height: Int, part: Int): FontChar? =
        ResourceLookups.wailaBackground.getOrNull(height - MIN_HEIGHT)?.getOrNull(part)
    
}

private val WAILA_BACKGROUND_SOURCE = ResourcePath(ResourceType.Texture, "nova", "font/waila/source")
private val WAILA_BACKGROUND_CHAR_ID = Key.key("nova", "waila_background")

private class WailaBackgroundNineSlice(
    private val source: BufferedImage
) {
    
    init {
        require(source.width >= 5 && source.height >= 5) { "WAILA background nine-slice texture must be at least 5x5 pixels" }
    }
    
    fun createStart(height: Int): BufferedImage = createGlyph(0, 2, 2, height)
    fun createPart(height: Int): BufferedImage = createGlyph(2, source.width - 2, 1, height)
    fun createEnd(height: Int): BufferedImage = createGlyph(source.width - 2, source.width, 2, height)
    
    private fun createGlyph(sourceX0: Int, sourceX1: Int, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        graphics.drawImage(source, 0, 0, width, 2, sourceX0, 0, sourceX1, 2, null)
        graphics.drawImage(source, 0, 2, width, height - 2, sourceX0, 2, sourceX1, source.height - 2, null)
        graphics.drawImage(source, 0, height - 2, width, height, sourceX0, source.height - 2, sourceX1, source.height, null)
        graphics.dispose()
        return result
    }
    
}

/**
 * Generates the WAILA background font from a nine-slice texture.
 */
class WailaBackgroundTask(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:waila",
    true
), PackTask {
    
    override val stage = BuildStage.POST_WORLD
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class)
    
    override suspend fun run() {
        if (!WailaManager.ENABLED)
            return
        
        val textureContent = builder.getBuildData<TextureContent>()
        val nineSlice = WailaBackgroundNineSlice(textureContent.getImage(WAILA_BACKGROUND_SOURCE))
        val lookup = ArrayList<List<FontChar>>()
        
        for (height in WailaBackgroundTextures.MIN_HEIGHT..WailaBackgroundTextures.MAX_HEIGHT) {
            val chars = ArrayList<FontChar>(3)
            fun add(path: String, image: BufferedImage) {
                val imagePath = ResourcePath(ResourceType.FontTexture, "nova", "waila_background/$height/$path.png")
                builder.writeImage(imagePath, image)
                chars += addEntry(WAILA_BACKGROUND_CHAR_ID, imagePath, image.height, 0)
            }
            
            add("start", nineSlice.createStart(height))
            add("part", nineSlice.createPart(height))
            add("end", nineSlice.createEnd(height))
            lookup += chars
        }
        
        ResourceLookups.wailaBackground = lookup
    }
    
}
