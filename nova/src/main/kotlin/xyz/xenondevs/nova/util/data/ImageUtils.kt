package xyz.xenondevs.nova.util.data

import org.joml.Vector2i
import org.joml.Vector2ic
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferInt
import java.awt.image.Raster
import java.awt.image.SinglePixelPackedSampleModel

internal object ImageUtils {
    
    private val ARGB_BIT_MASKS = intArrayOf(0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000.toInt())
    
    /**
     * Finds the left and right borders of the given [image].
     * The values in the returned Pair<Left, Right> correspond with the
     * x-coordinate of the first non-empty column from the left and right side of the image.
     *
     * @return An integer pair containing the left and right borders, or null if the image is completely empty.
     */
    fun findVerticalBorders(image: BufferedImage): Pair<Int, Int>? {
        var left = 0
        while (left < image.width && isColumnEmpty(image, left)) left++
        if (left == image.width) return null
        
        var right = image.width - 1
        while (right > left && isColumnEmpty(image, right)) right--
        
        return left to right
    }
    
    /**
     * Finds the left border of the given [image].
     * The returned value corresponds with the x-coordinate of the first non-empty column from the left side of the image
     * or null if the image is completely empty.
     */
    fun findLeftBorder(image: BufferedImage): Int? {
        var x = 0
        while (x < image.width && isColumnEmpty(image, x)) x++
        return if (x != image.width) x else null
    }
    
    /**
     * Finds the right border of the given [image].
     * The returned value corresponds with the x-coordinate of the first non-empty column from the right side of the image
     * or null if the image is completely empty.
     */
    fun findRightBorder(image: BufferedImage): Int? {
        var x = image.width - 1
        while (x >= 0 && isColumnEmpty(image, x)) x--
        return if (x != -1) x else null
    }
    
    /**
     * Checks whether the column at the given [x] coordinate is transparent.
     */
    fun isColumnEmpty(image: BufferedImage, x: Int): Boolean {
        for (y in 0..<image.height) {
            if (image.getRGB(x, y) ushr 24 != 0)
                return false
        }
        return true
    }
    
    /**
     * Finds the top and bottom borders of the given [image].
     * The values in the returned Pair<Top, Bottom> correspond with the
     * y-coordinate of the first non-empty row from the top and bottom side of the image.
     *
     * @return A vector containing the top and bottom borders, or null if the image is completely empty.
     */
    fun findTopBottomBorders(image: BufferedImage): Vector2ic? {
        var top = 0
        while (top < image.height && isRowEmpty(image, top)) top++
        if (top == image.height) return null
        
        var bottom = image.height - 1
        while (bottom > top && isRowEmpty(image, bottom)) bottom--
        
        return Vector2i(top, bottom)
    }
    
    /**
     * Finds the bottom border of the given [image].
     * The returned value corresponds with the y-coordinate of the first non-empty row from the bottom side of the image
     * or null if the image is completely empty.
     */
    fun findTopBorder(image: BufferedImage): Int? {
        var y = 0
        while (y < image.height && isRowEmpty(image, y)) y++
        return if (y != image.height) y else null
    }
    
    /**
     * Finds the bottom border of the given [image].
     * The returned value corresponds with the y-coordinate of the first non-empty row from the bottom side of the image
     * or null if the image is completely empty.
     */
    fun findBottomBorder(image: BufferedImage): Int? {
        var y = image.height - 1
        while (y >= 0 && isRowEmpty(image, y)) y--
        return if (y != -1) y else null
    }
    
    /**
     * Checks whether the row at the given [y] coordinate is transparent.
     */
    fun isRowEmpty(image: BufferedImage, y: Int): Boolean {
        for (x in 0..<image.width) {
            if (image.getRGB(x, y) ushr 24 != 0)
                return false
        }
        return true
    }
    
    @JvmStatic
    fun createImageFromArgbRaster(width: Int, raster: IntArray): BufferedImage {
        // https://stackoverflow.com/questions/14416107/int-array-to-bufferedimage
        val sm = SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, 16, ARGB_BIT_MASKS)
        val db = DataBufferInt(raster, raster.size)
        val wr = Raster.createWritableRaster(sm, db, Point())
        return BufferedImage(ColorModel.getRGBdefault(), wr, false, null)
    }
    
}