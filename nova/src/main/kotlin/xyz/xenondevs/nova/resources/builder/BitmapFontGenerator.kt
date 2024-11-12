package xyz.xenondevs.nova.resources.builder

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.FontProvider
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.SpaceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.ArrayCodePointGrid
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.RasterGlyphGrid
import xyz.xenondevs.nova.resources.builder.font.provider.unihex.UnihexProvider

private const val GLYPH_HEIGHT = 16

/**
 * Generates bitmap fonts from fonts that contain both 'bitmap' and 'unihex' font providers.
 * Keeps reference and space providers.
 *
 * @param font The font to generate the bitmap font for.
 */
internal class BitmapFontGenerator(
    private val font: Font
) {
    
    /**
     * Creates a new [Font] under the same id as the original [font], with the same [ReferenceProviders][ReferenceProvider],
     * [SpaceProviders][SpaceProvider] and [BitmapProviders][BitmapProvider], but with all [UnihexProviders][UnihexProvider]
     * converted to [BitmapProviders][BitmapProvider].
     */
    fun generateBitmapFont(): Font {
        LOGGER.info("Creating a bitmap font for $font")
        
        val providers = ArrayList<FontProvider>()
        for (provider in font.providers) {
            when (provider) {
                is ReferenceProvider, is SpaceProvider, is BitmapProvider<*> -> providers.add(provider)
                is UnihexProvider -> providers.addAll(convertUnihexProvider(provider))
                else -> LOGGER.warn("Skipping unsupported font provider type: ${provider::class.simpleName}")
            }
        }
        
        return Font(font.id, providers)
    }
    
    /**
     * Converts the given [UnihexProvider] to a list of [BitmapProviders][BitmapProvider].
     *
     * Also writes their bitmap textures to the resource pack build directory.
     */
    private fun convertUnihexProvider(provider: UnihexProvider): List<BitmapProvider<IntArray>> =
        provider.glyphRasters.map { (width, glyphRasters) ->
            val bitmapProvider = buildBitmapProvider(width, glyphRasters)
            bitmapProvider.write(ResourcePackBuilder.ASSETS_DIR)
            return@map bitmapProvider
        }
    
    /**
     * Builds a bitmap provider for the given [glyphRasters] of [width].
     * The resulting rasters will only have one row.
     */
    private fun buildBitmapProvider(width: Int, glyphRasters: Int2ObjectMap<IntArray>): BitmapProvider<IntArray> {
        val glyphCount = glyphRasters.size
        val rasterWidth = width * glyphCount // the length of one line in the resulting raster
        
        val codePoints = IntArray(glyphCount)
        val raster = IntArray(rasterWidth * GLYPH_HEIGHT) // expects glyph height of 16px
        
        // fill raster array with all glyph rasters, fill codePoints array with all code points
        glyphRasters.int2ObjectEntrySet().asSequence()
            .sortedBy { it.intKey } // sorting the glyphs by code point decreases file size by about 30% due to better png compression
            .withIndex()
            .forEach { (i, entry) ->
                codePoints[i] = entry.intKey
                val glyphRaster = entry.value
                for (y in 0..<GLYPH_HEIGHT) {
                    System.arraycopy(glyphRaster, y * width, raster, y * rasterWidth + i * width, width)
                }
            }
        
        val id = font.id
        return BitmapProvider.custom(
            ResourcePath(font.id.namespace, "font/${id.path}/nova_bmp/${width}x16.png"),
            ArrayCodePointGrid(arrayOf(codePoints)), RasterGlyphGrid(glyphCount, 1, width, 16, raster),
            8, 7
        )
    }
    
}