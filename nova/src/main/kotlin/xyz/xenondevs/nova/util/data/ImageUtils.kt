package xyz.xenondevs.nova.util.data

import java.awt.Color
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentSampleModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.awt.image.Raster
import java.awt.image.SinglePixelPackedSampleModel

internal data class ImageBorders(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)

internal object ImageUtils {
    
    private val ARGB_BIT_MASKS = intArrayOf(0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000.toInt())
    
    /**
     * Finds all non-transparent borders of [image] in one pass.
     */
    fun findBorders(image: BufferedImage): ImageBorders? {
        val width = image.width
        val height = image.height
        if (width == 0 || height == 0)
            return null
        if (!image.colorModel.hasAlpha())
            return ImageBorders(0, width - 1, 0, height - 1)
        
        val alphaRaster = image.alphaRaster
        if (alphaRaster != null) {
            val sampleModel = alphaRaster.sampleModel
            val sampleX = alphaRaster.minX - alphaRaster.sampleModelTranslateX
            val sampleY = alphaRaster.minY - alphaRaster.sampleModelTranslateY
            
            if (sampleModel is ComponentSampleModel && alphaRaster.dataBuffer is DataBufferByte) {
                val dataBuffer = alphaRaster.dataBuffer as DataBufferByte
                val bank = sampleModel.bankIndices[0]
                val data = dataBuffer.bankData[bank]
                val offset = dataBuffer.offsets[bank] + sampleModel.getOffset(sampleX, sampleY)
                return findByteBorders(data, offset, sampleModel.pixelStride, sampleModel.scanlineStride, width, height)
            }
            
            if (sampleModel is SinglePixelPackedSampleModel && alphaRaster.dataBuffer is DataBufferInt) {
                val dataBuffer = alphaRaster.dataBuffer as DataBufferInt
                val data = dataBuffer.bankData[0]
                val offset = dataBuffer.offsets[0] + sampleModel.getOffset(sampleX, sampleY)
                return findIntBorders(data, offset, sampleModel.scanlineStride, sampleModel.bitMasks[0], width, height)
            }
            
            return findRasterBorders(alphaRaster, width, height)
        }
        
        return findColorModelBorders(image)
    }
    
    /**
     * Finds all non-transparent borders of the row-major ARGB [image] with the given [width] and [height] in one pass.
     */
    fun findBorders(image: IntArray, width: Int, height: Int): ImageBorders? {
        requireDimensions(image, width, height)
        return findIntBorders(image, 0, width, 0xFF000000.toInt(), width, height)
    }
    
    private fun requireDimensions(image: IntArray, width: Int, height: Int) {
        require(width >= 0 && height >= 0) { "Image dimensions must be non-negative" }
        require(image.size == width * height) {
            "ARGB array size ${image.size} does not match image dimensions $width x $height"
        }
    }
    
    private fun findByteBorders(
        image: ByteArray,
        offset: Int,
        pixelStride: Int,
        scanlineStride: Int,
        width: Int,
        height: Int
    ): ImageBorders? {
        var left = 0
        left@ while (left < width) {
            var index = offset + left * pixelStride
            repeat(height) {
                if (image[index].toInt() and 0xFF != 0)
                    break@left
                index += scanlineStride
            }
            left++
        }
        if (left == width)
            return null
        
        var right = width - 1
        right@ while (right > left) {
            var index = offset + right * pixelStride
            repeat(height) {
                if (image[index].toInt() and 0xFF != 0)
                    break@right
                index += scanlineStride
            }
            right--
        }
        
        var top = 0
        top@ while (top < height) {
            var index = offset + top * scanlineStride + left * pixelStride
            repeat(right - left + 1) {
                if (image[index].toInt() and 0xFF != 0)
                    break@top
                index += pixelStride
            }
            top++
        }
        
        var bottom = height - 1
        bottom@ while (bottom > top) {
            var index = offset + bottom * scanlineStride + left * pixelStride
            repeat(right - left + 1) {
                if (image[index].toInt() and 0xFF != 0)
                    break@bottom
                index += pixelStride
            }
            bottom--
        }
        
        return ImageBorders(left, right, top, bottom)
    }
    
    private fun findIntBorders(
        image: IntArray,
        offset: Int,
        scanlineStride: Int,
        alphaMask: Int,
        width: Int,
        height: Int
    ): ImageBorders? {
        var left = 0
        left@ while (left < width) {
            var index = offset + left
            repeat(height) {
                if (image[index] and alphaMask != 0)
                    break@left
                index += scanlineStride
            }
            left++
        }
        if (left == width)
            return null
        
        var right = width - 1
        right@ while (right > left) {
            var index = offset + right
            repeat(height) {
                if (image[index] and alphaMask != 0)
                    break@right
                index += scanlineStride
            }
            right--
        }
        
        var top = 0
        top@ while (top < height) {
            var index = offset + top * scanlineStride + left
            repeat(right - left + 1) {
                if (image[index] and alphaMask != 0)
                    break@top
                index++
            }
            top++
        }
        
        var bottom = height - 1
        bottom@ while (bottom > top) {
            var index = offset + bottom * scanlineStride + left
            repeat(right - left + 1) {
                if (image[index] and alphaMask != 0)
                    break@bottom
                index++
            }
            bottom--
        }
        
        return ImageBorders(left, right, top, bottom)
    }
    
    private fun findRasterBorders(alphaRaster: Raster, width: Int, height: Int): ImageBorders? {
        var left = 0
        left@ while (left < width) {
            for (y in 0..<height) {
                if (alphaRaster.getSample(alphaRaster.minX + left, alphaRaster.minY + y, 0) != 0)
                    break@left
            }
            left++
        }
        if (left == width)
            return null
        
        var right = width - 1
        right@ while (right > left) {
            for (y in 0..<height) {
                if (alphaRaster.getSample(alphaRaster.minX + right, alphaRaster.minY + y, 0) != 0)
                    break@right
            }
            right--
        }
        
        var top = 0
        top@ while (top < height) {
            for (x in left..right) {
                if (alphaRaster.getSample(alphaRaster.minX + x, alphaRaster.minY + top, 0) != 0)
                    break@top
            }
            top++
        }
        
        var bottom = height - 1
        bottom@ while (bottom > top) {
            for (x in left..right) {
                if (alphaRaster.getSample(alphaRaster.minX + x, alphaRaster.minY + bottom, 0) != 0)
                    break@bottom
            }
            bottom--
        }
        
        return ImageBorders(left, right, top, bottom)
    }
    
    private fun findColorModelBorders(image: BufferedImage): ImageBorders? {
        val raster = image.raster
        val colorModel = image.colorModel
        var pixel: Any? = null
        
        var left = 0
        left@ while (left < image.width) {
            for (y in 0..<image.height) {
                pixel = raster.getDataElements(raster.minX + left, raster.minY + y, pixel)
                if (colorModel.getAlpha(pixel) != 0)
                    break@left
            }
            left++
        }
        if (left == image.width)
            return null
        
        var right = image.width - 1
        right@ while (right > left) {
            for (y in 0..<image.height) {
                pixel = raster.getDataElements(raster.minX + right, raster.minY + y, pixel)
                if (colorModel.getAlpha(pixel) != 0)
                    break@right
            }
            right--
        }
        
        var top = 0
        top@ while (top < image.height) {
            for (x in left..right) {
                pixel = raster.getDataElements(raster.minX + x, raster.minY + top, pixel)
                if (colorModel.getAlpha(pixel) != 0)
                    break@top
            }
            top++
        }
        
        var bottom = image.height - 1
        bottom@ while (bottom > top) {
            for (x in left..right) {
                pixel = raster.getDataElements(raster.minX + x, raster.minY + bottom, pixel)
                if (colorModel.getAlpha(pixel) != 0)
                    break@bottom
            }
            bottom--
        }
        
        return ImageBorders(left, right, top, bottom)
    }
    
    @JvmStatic
    fun createImageFromArgbRaster(width: Int, raster: IntArray): BufferedImage {
        // https://stackoverflow.com/questions/14416107/int-array-to-bufferedimage
        val sm = SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, raster.size / width, ARGB_BIT_MASKS)
        val db = DataBufferInt(raster, raster.size)
        val wr = Raster.createWritableRaster(sm, db, Point())
        return BufferedImage(ColorModel.getRGBdefault(), wr, false, null)
    }
    
    /**
     * Makes all semi-transparent pixels with alpha < 128 completely transparent
     * and all semi-transparent pixels with alpha >= 128 completely opaque.
     */
    fun removeSemiTransparentPixels(image: BufferedImage) {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getRGB(x, y)
                val alpha = rgb ushr 24
                if (alpha == 0 || alpha == 255)
                    continue
                
                if (alpha > 127) {
                    image.setRGB(x, y, rgb or (0xFF000000).toInt())
                } else {
                    image.setRGB(x, y, 0)
                }
            }
        }
    }
    
    /**
     * Applies a linear interpolation to all pixels of the given [from] and [to] images.
     */
    fun lerp(from: BufferedImage, to: BufferedImage, blendFactor: Float): BufferedImage {
        val width = from.width
        val height = from.height
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val fromARGB = from.getRGB(x, y)
                val toARGB = to.getRGB(x, y)
                
                val fromA = fromARGB ushr 24 and 0xFF
                val fromR = fromARGB ushr 16 and 0xFF
                val fromG = fromARGB ushr 8 and 0xFF
                val fromB = fromARGB and 0xFF
                
                val toA = toARGB ushr 24 and 0xFF
                val toR = toARGB ushr 16 and 0xFF
                val toG = toARGB ushr 8 and 0xFF
                val toB = toARGB and 0xFF
                
                val resultA = (fromA + (toA - fromA) * blendFactor).toInt()
                val resultR = (fromR + (toR - fromR) * blendFactor).toInt()
                val resultG = (fromG + (toG - fromG) * blendFactor).toInt()
                val resultB = (fromB + (toB - fromB) * blendFactor).toInt()
                
                val resultARGB = resultA shl 24 or (resultR shl 16) or (resultG shl 8) or resultB
                result.setRGB(x, y, resultARGB)
            }
        }
        
        return result
    }
    
    /**
     * Linearly interpolates between [from] and [to] using the given [blendFactor].
     */
    fun lerp(from: Color, to: Color, blendFactor: Float): Color {
        val fromA = from.alpha
        val fromR = from.red
        val fromG = from.green
        val fromB = from.blue
        
        val toA = to.alpha
        val toR = to.red
        val toG = to.green
        val toB = to.blue
        
        val resultA = (fromA + (toA - fromA) * blendFactor).toInt()
        val resultR = (fromR + (toR - fromR) * blendFactor).toInt()
        val resultG = (fromG + (toG - fromG) * blendFactor).toInt()
        val resultB = (fromB + (toB - fromB) * blendFactor).toInt()
        
        return Color(resultR, resultG, resultB, resultA)
    }
    
    /**
     * Creates a copy of [image] into [BufferedImage.TYPE_INT_ARGB].
     */
    fun copyToARGB(image: BufferedImage): BufferedImage {
        val copy = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = copy.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return copy
    }
    
}