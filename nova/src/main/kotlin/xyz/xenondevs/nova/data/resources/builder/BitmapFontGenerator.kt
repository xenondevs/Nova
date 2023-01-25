package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import xyz.xenondevs.commons.gson.addAll
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.data.readImage
import xyz.xenondevs.nova.util.data.writeImage
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.math.ceil

private const val UNICODE_PAGE_FORMAT = "%02x"
private const val FONT_TEXTURE_GLYPHS_PER_ROW = 16

/**
 * Generates bitmap fonts from fonts that contain both 'bitmap' and 'legacy_unicode' font providers.
 *
 * @param font The font to generate the bitmap font for.
 * @param removeBitmapProviders If existing bitmap font providers should be removed.
 */
internal class BitmapFontGenerator(
    private val font: ResourcePath,
    private val removeBitmapProviders: Boolean
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
            val type = provider.getStringOrNull("type")
            if (type == "legacy_unicode") {
                outProviders.addAll(convertLegacyUnicodeProvider(provider))
            } else if (!removeBitmapProviders || type != "bitmap") {
                outProviders.add(provider)
            }
        }
        
        return outFont
    }
    
    /**
     * Converts the given `legacy_unicode` font [provider] to a list of `bitmap` providers.
     * Also creates the required font textures.
     */
    private fun convertLegacyUnicodeProvider(provider: JsonObject): List<JsonObject> {
        val sizesPath = ResourcePath.of(provider.getStringOrNull("sizes")!!, "minecraft")
        val textureTemplate = provider.getStringOrNull("template")!!
        
        val glyphSizes: ByteBuf = Unpooled.wrappedBuffer(sizesPath.findInAssets().readBytes())
        val unicodePages: Array<BufferedImage?> = Array(256) {
            val hexPageNum = UNICODE_PAGE_FORMAT.format(it)
            val path = ResourcePath.of(textureTemplate.format(hexPageNum)).findInAssetsOrNull("textures")
            path?.let(Path::readImage)
        }
        
        val sortedGlyphs = extractAndSortGlyphs(glyphSizes, unicodePages)
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
     * Reads all glyphs present in the [glyphSizes] buffer, extracts them from their [unicodePage][unicodePages]
     * and sorts them by their width.
     *
     * @return A map in the format `Map<Width, Map<Char Code, Glyph Image>>`
     */
    private fun extractAndSortGlyphs(glyphSizes: ByteBuf, unicodePages: Array<BufferedImage?>): Map<Int, Map<Int, BufferedImage>> {
        val glyphs = HashMap<Int, HashMap<Int, BufferedImage>>()
        
        for (c in 0..0xFFFF) {
            val pageNumber = c shr 8
            val row = c shr 4 and 0xF
            val colum = c and 0xF
            
            val size = glyphSizes.readUnsignedByte().toInt()
            val start = size shr 4 and 0xF
            val end = size and 0xF
            val width = end - start + 1
            
            val page = unicodePages[pageNumber] ?: continue
            
            val widthMap = glyphs.getOrPut(width, ::HashMap)
            widthMap[c] = page.getSubimage(colum * 16 + start, row * 16, width, 16)
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
        
        val sb = StringBuilder()
        glyphs.keys.forEach { charCode ->
            sb.append(charCode.toChar())
            if (sb.length == FONT_TEXTURE_GLYPHS_PER_ROW) {
                chars.add(sb.toString())
                sb.clear()
            }
        }
        
        if (sb.isNotEmpty()) {
            repeat(FONT_TEXTURE_GLYPHS_PER_ROW - sb.length) {
                sb.append("\u0000")
            }
            chars.add(sb.toString())
        }
        
        return provider
    }
    
}