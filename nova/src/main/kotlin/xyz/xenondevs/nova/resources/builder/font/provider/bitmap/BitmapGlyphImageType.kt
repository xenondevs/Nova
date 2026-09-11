package xyz.xenondevs.nova.resources.builder.font.provider.bitmap

import xyz.xenondevs.nova.util.data.ImageBorders
import xyz.xenondevs.nova.util.data.ImageUtils
import java.awt.image.BufferedImage

/**
 * An interface for dealing with different types of glyph texture types for [BitmapProviders][BitmapProvider].
 */
internal interface BitmapGlyphImageType<T> {
    
    /**
     * Finds all non-transparent borders of [image].
     */
    fun findBorders(image: T, width: Int, height: Int): ImageBorders?
    
    companion object {
        
        /**
         * A glyph texture represented by a one-dimensional array of ARGB integers.
         */
        val ARGB_ARRAY: BitmapGlyphImageType<IntArray> = object : BitmapGlyphImageType<IntArray> {
            
            override fun findBorders(image: IntArray, width: Int, height: Int): ImageBorders? =
                ImageUtils.findBorders(image, width, height)
            
        }
        
        /**
         * A glyph texture represented by a [BufferedImage].
         */
        val BUFFERED_IMAGE: BitmapGlyphImageType<BufferedImage> = object : BitmapGlyphImageType<BufferedImage> {
            override fun findBorders(image: BufferedImage, width: Int, height: Int): ImageBorders? {
                require(image.width == width && image.height == height) {
                    "BufferedImage dimensions ${image.width} x ${image.height} do not match glyph dimensions $width x $height"
                }
                return ImageUtils.findBorders(image)
            }
        }
        
    }
    
}