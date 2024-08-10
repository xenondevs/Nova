package xyz.xenondevs.nova.resources.builder.font.provider.bitmap

import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.nova.util.data.ImageUtils
import java.awt.image.BufferedImage

/**
 * An interface for dealing with different types of glyph texture types for [BitmapProviders][BitmapProvider].
 */
interface BitmapGlyphImageType<T> {
    
    /**
     * Finds the right border of the given [image].
     * The returned value corresponds with the x-coordinate of the first non-empty column from the right side of the image
     * or null if the image is completely empty.
     */
    fun findRightBorder(image: T, width: Int, height: Int): Int?
    
    /**
     * Finds the top and bottom borders of the given [image].
     * The values in the returned Pair<Top, Bottom> correspond with the
     * y-coordinate of the first non-empty row from the top and bottom side of the image.
     *
     * @return A vector containing the top and bottom borders, or null if the image is completely empty.
     */
    fun findTopBottomBorders(image: T, width: Int, height: Int): Vector2ic?
    
    companion object {
        
        /**
         * A glyph texture represented by a one-dimensional array of argb integers.
         */
        val ARGB_ARRAY: BitmapGlyphImageType<IntArray> = object : BitmapGlyphImageType<IntArray> {
            
            // TODO: consider moving these to ImageUtils as well?
            override fun findRightBorder(image: IntArray, width: Int, height: Int): Int? {
                fun isColumnEmpty(x: Int): Boolean {
                    for (y in 0..<height) {
                        if (image[y * width + x] ushr 24 != 0)
                            return false
                    }
                    return true
                }
                
                var x = width - 1
                while (x >= 0 && isColumnEmpty(x)) x--
                return if (x != -1) x else null
            }
            
            override fun findTopBottomBorders(image: IntArray, width: Int, height: Int): Vector2ic? {
                fun isRowEmpty(y: Int): Boolean {
                    for (x in 0..<width) {
                        if (image[y * width + x] ushr 24 != 0)
                            return false
                    }
                    return true
                }
                
                var top = 0
                while (top < height && isRowEmpty(top)) top++
                if (top == height) return null
                
                var bottom = height - 1
                while (bottom > top && isRowEmpty(bottom)) bottom--
                
                return Vector2i(top, bottom)
            }
            
        }
        
        /**
         * A glyph texture represented by a [BufferedImage].
         */
        val BUFFERED_IMAGE: BitmapGlyphImageType<BufferedImage> = object : BitmapGlyphImageType<BufferedImage> {
            override fun findRightBorder(image: BufferedImage, width: Int, height: Int): Int? = ImageUtils.findRightBorder(image)
            override fun findTopBottomBorders(image: BufferedImage, width: Int, height: Int): Vector2ic? = ImageUtils.findTopBottomBorders(image)
        }
        
    }
    
}