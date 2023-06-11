package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.addAll
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.data.font.UnihexProvider
import xyz.xenondevs.nova.util.data.writeImage
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.math.ceil

private const val FONT_TEXTURE_GLYPHS_PER_ROW = 16

/**
 * Generates bitmap fonts from fonts that contain both 'bitmap' and 'unihex' font providers.
 * Keeps reference and space providers.
 *
 * @param font The font to generate the bitmap font for.
 */
internal class BitmapFontGenerator(
    private val font: ResourcePath
) {
    
    private val fontFile: Path = font.findInAssets("font", "json")
    private val fontObj: JsonObject = fontFile.parseJson() as JsonObject
    
    fun generateBitmapFont(): JsonObject {
        LOGGER.info("Creating a bitmap font for $font")
        
        val outFont = JsonObject()
        val outProviders = JsonArray().also { outFont.add("providers", it) }
        
        val inProviders = fontObj.getAsJsonArray("providers")
        inProviders.forEach { provider ->
            provider as JsonObject
            when (val type = provider.getStringOrNull("type")) {
                "unihex" -> outProviders.addAll(convertUnihexProvider(provider))
                "bitmap", "space", "reference" -> outProviders.add(provider)
                else -> LOGGER.warning("Skipping unsupported font provider type: $type")
            }
        }
        
        return outFont
    }
    
    /**
     * Converts the given `unihex` font [provider] to a list of `bitmap` providers and
     * creates the required font textures.
     */
    private fun convertUnihexProvider(provider: JsonObject): List<JsonObject> {
        val sortedGlyphs = extractAndSortGlyphs(UnihexProvider.of(provider))
        return sortedGlyphs.map { (width, glyphs) ->
            val texture = buildFontTexture(width, glyphs)
            
            val texturePath = ResourcePath(font.namespace, "font/${font.path}/nova_bmp/${width}x16.png")
            val file = texturePath.getPath(ResourcePackBuilder.ASSETS_DIR, "textures")
            file.parent.createDirectories()
            file.writeImage(texture, "PNG")
            
            return@map buildBitmapProvider(texturePath, glyphs)
        }
    }
    
    /**
     * Reads all glyphs from the given [hexFiles], draws them to a [BufferedImage], and sorts them by width.
     *
     * @return A map in the format `Map<Width, Map<Codepoint, Glyph Image>>`
     */
    private fun extractAndSortGlyphs(unihexProvider: UnihexProvider): Map<Int, Map<Int, BufferedImage>> {
        val glyphs = HashMap<Int, LinkedHashMap<Int, BufferedImage>>()
        
        unihexProvider.glyphSequence().forEach { glyph ->
            val (left, right) = glyph.findHorizontalBounds() ?: return@forEach
            
            var img = glyph.img
            if (left > 0 || right < glyph.width) {
                img = img.getSubimage(left, 0, right - left, 16)
            }
            
            glyphs.getOrPut(img.width, ::LinkedHashMap)[glyph.codePoint] = img
        }
        
        return glyphs
    }
    
    /**
     * Creates a new [BufferedImage] font texture from the given [glyphs] of the given [width].
     */
    private fun buildFontTexture(width: Int, glyphs: Map<Int, BufferedImage>): BufferedImage {
        val rows = ceil(glyphs.size.toDouble() / FONT_TEXTURE_GLYPHS_PER_ROW.toDouble()).toInt()
        val image = BufferedImage(width * 16, 16 * rows, BufferedImage.TYPE_INT_ARGB)
        
        var column = 0
        var row = 0
        glyphs.values.forEach { img ->
            val rgbArray = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
            image.setRGB(column * width, row * 16, width, 16, rgbArray, 0, width)
            
            column++
            if (column == 16) {
                column = 0
                row++
            }
        }
        
        return image
    }
    
    /**
     * Creates a new `bitmap` provider [JsonObject] for the given [glyphs] that uses the font texture under [texturePath].
     */
    private fun buildBitmapProvider(texturePath: ResourcePath, glyphs: Map<Int, BufferedImage>): JsonObject {
        val chars = JsonArray()
        val provider = JsonObject().apply {
            addProperty("type", "bitmap")
            addProperty("file", texturePath.toString())
            addProperty("ascent", 7)
            addProperty("height", 8)
            add("chars", chars)
        }
        
        var charsInRow = 0
        val sb = StringBuilder()
        glyphs.keys.forEach { codePoint ->
            sb.appendCodePoint(codePoint)
            if (++charsInRow == FONT_TEXTURE_GLYPHS_PER_ROW) {
                chars.add(sb.toString())
                sb.clear()
                charsInRow = 0
            }
        }
        
        if (charsInRow != 0) {
            repeat(FONT_TEXTURE_GLYPHS_PER_ROW - charsInRow) { sb.appendCodePoint(0) }
            chars.add(sb.toString())
        }
        
        return provider
    }
    
}