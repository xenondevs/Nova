package xyz.xenondevs.nova.util.data

import java.awt.image.BufferedImage

object ImageUtils {
    
    /**
     * Finds the left and right borders of the given [image].
     * The values in the returned Pair<Left, Right> correspond with the
     * x-coordinate of the first non-empty column from the left and right side of the image.
     *
     * @return An integer pair containing the left and right borders, or null if the image is completely empty.
     */
    fun findHorizontalBorders(image: BufferedImage): Pair<Int, Int>? {
        fun columnEmpty(x: Int): Boolean {
            for (y in 0 until image.height) {
                if (image.getRGB(x, y) ushr 24 != 0)
                    return false
            }
            return true
        }
        
        var left = 0
        while (left < image.width && columnEmpty(left)) left++
        if (left == image.width) return null
        
        var right = image.width - 1
        while (right > left && columnEmpty(right)) right--
        
        return left to right
    }
    
    /**
     * Finds the top and bottom borders of the given [image].
     * The values in the returned Pair<Top, Bottom> correspond with the
     * y-coordinate of the first non-empty row from the top and bottom side of the image.
     * 
     * @return An integer pair containing the top and bottom borders, or null if the image is completely empty.
     */
    fun findVerticalBorders(image: BufferedImage): Pair<Int, Int>? {
        fun rowEmpty(y: Int): Boolean {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr 24 != 0)
                    return false
            }
            return true
        }
        
        var top = 0
        while (top < image.height && rowEmpty(top)) top++
        if (top == image.height) return null
        
        var bottom = image.height - 1
        while (bottom > top && rowEmpty(bottom)) bottom--
        
        return top to bottom
    }
    
    
    
}