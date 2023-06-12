package xyz.xenondevs.nova.util.data.font

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getArrayOrNull
import xyz.xenondevs.commons.gson.getInt
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.data.ImageUtils
import xyz.xenondevs.nova.util.data.font.FontUtils.readUnihexFiles
import java.awt.image.BufferedImage
import java.util.*

/**
 * Represents a unihex font provider.
 *
 * @param files The content of all .hex files. Each string contains multiple lines, each line represents a glyph.
 * @param sizeOverrides The size overrides
 */
internal data class UnihexProvider(val files: List<String>, val sizeOverrides: List<SizeOverride>) {
    
    /**
     * Creates a sequence of all glyphs in this provider.
     */
    fun glyphSequence(): Sequence<Glyph> =
        files.asSequence()
            .flatMap(String::lineSequence)
            .mapNotNull { line ->
                val parts = line.split(':')
                if (parts.size != 2)
                    return@mapNotNull null
                
                val codePoint = HexFormat.fromHexDigits(parts[0])
                val hexStr = parts[1]
                val width = hexStr.length * 4 / 16 // 4 bits per hex digit, 16 lines per glyph
                
                val img = BufferedImage(width, 16, BufferedImage.TYPE_INT_ARGB)
                FontUtils.getBitSequence(hexStr).withIndex().forEach { (idx, bit) ->
                    if (bit) img.setRGB(idx % width, idx / width, 0xFFFFFFFF.toInt())
                }
                
                return@mapNotNull Glyph(codePoint, width, img)
            }
    
    /**
     * Represents a single character / glyph in a unihex font provider.
     */
    inner class Glyph(val codePoint: Int, val width: Int, val img: BufferedImage) {
        
        /**
         * Calculates the left and right bounds, which are the first and last non-empty columns, of the glyph image
         * or returns null if the glyph image is empty.
         */
        fun findHorizontalBounds(): Pair<Int, Int>? {
            val sizeOverride = sizeOverrides.firstOrNull { codePoint in it.from..it.to }
            
            val left: Int
            val right: Int
            if (sizeOverride == null) {
                val borders = ImageUtils.findHorizontalBorders(img) ?: return null
                left = borders.first
                right = borders.second + 1
            } else {
                left = sizeOverride.left
                right = sizeOverride.right
            }
            
            return left to right
        }
        
        fun findVerticalBounds(): Pair<Int, Int>? =
            ImageUtils.findVerticalBorders(img)
        
    }
    
    companion object {
        
        fun of(provider: JsonObject) = UnihexProvider(
            readUnihexFiles(ResourcePath.of(provider.getString("hex_file"), "minecraft").findInAssets()),
            provider.getArrayOrNull("size_overrides")?.map {
                it as JsonObject
                SizeOverride(it.getString("from"), it.getString("to"), it.getInt("left"), it.getInt("right"))
            } ?: emptyList()
        )
        
    }
    
}