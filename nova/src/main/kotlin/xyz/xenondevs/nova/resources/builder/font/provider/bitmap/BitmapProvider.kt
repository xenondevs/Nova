package xyz.xenondevs.nova.resources.builder.font.provider.bitmap

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.toJsonArray
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.font.provider.FontProvider
import xyz.xenondevs.nova.util.data.readImage
import xyz.xenondevs.nova.util.data.writeImage
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.math.roundToInt

/**
 * Represents a `bitmap` font provider.
 */
abstract class BitmapProvider<T> internal constructor() : FontProvider() {
    
    /**
     * A [ResourcePath] to the texture file.
     */
    abstract val file: ResourcePath
    
    /**
     * The height of the characters in this [BitmapProvider].
     * If this does not match the height of the glyphs, the glyphs will be scaled, keeping the aspect ratio.
     * This is the `height` property in json.
     */
    abstract val height: Int
    
    /**
     * The defined character ascent. This is the `ascent` property in json.
     */
    abstract val ascent: Int
    
    /**
     * The [BitmapGlyphImageType] of this [BitmapProvider]. Used to calculate char sizes.
     */
    protected abstract val glyphImageType: BitmapGlyphImageType<T>
    
    /**
     * The code points, aligned in a grid pointing to the glyphs,
     */
    protected abstract val codePointGrid: CodePointGrid
    
    /**
     * The glyphs, aligned in a grid pointing to the code points.
     */
    protected abstract val glyphGrid: GlyphGrid<T>
    
    // This cannot be cached because then we'd have to loop over the grid every time it is modified
    // to check whether the code point is still present somewhere
    final override val codePoints: IntSet
        get() = codePointGrid.getCodePoints()
    
    override val charSizes: Int2ObjectMap<FloatArray> by lazy(::calculateCharSizes)
    
    override fun write(assetsDir: Path) {
        val file = file.getPath(assetsDir, "textures")
        file.parent.createDirectories()
        file.writeImage(glyphGrid.toImage(), "PNG")
    }
    
    override fun toJson() = JsonObject().apply {
        addProperty("type", "bitmap")
        addProperty("file", file.toString())
        if (height != 8) addProperty("height", height)
        addProperty("ascent", ascent)
        add("chars", codePointGrid.toStringList().toJsonArray())
    }
    
    private fun calculateCharSizes(): Int2ObjectMap<FloatArray> {
        val map = Int2ObjectOpenHashMap<FloatArray>()
        
        val glyphImageType = glyphImageType
        val codePointGrid = codePointGrid
        val glyphGrid = glyphGrid
        val glyphWidth = glyphGrid.glyphWidth
        val glyphHeight = glyphGrid.glyphHeight
        val height = height
        val ascent = ascent
        
        for (x in 0..<codePointGrid.width) {
            for (y in 0..<codePointGrid.height) {
                val codePoint = codePointGrid[x, y]
                if (codePoint != 0) {
                    val glyph = glyphGrid[x, y]
                        ?: throw IllegalStateException("No glyph present for registered code point $codePoint")
                    
                    val rescale = height / glyphHeight.toFloat()
                    
                    var width = glyphImageType.findRightBorder(glyph, glyphWidth, glyphHeight)
                        ?.let { rightBorder -> ((rightBorder + 1) * rescale).roundToInt().toFloat() }
                        ?: 0f
                    
                    width += 1f // +1 to include space between characters
                    if (width < 0) width += 1f
                    
                    var minY = 0f
                    var maxY = 0f
                    val horizontalBorders = glyphImageType.findTopBottomBorders(glyph, glyphWidth, glyphHeight)
                    if (horizontalBorders != null) {
                        minY = ((horizontalBorders.x() - ascent) * rescale)
                        maxY = ((horizontalBorders.y() - ascent) * rescale)
                    }
                    
                    map.put(codePoint, floatArrayOf(width, minY, maxY))
                }
            }
        }
        
        return map
    }
    
    /**
     * An immutable [BitmapProvider] referencing another [BitmapProvider] while overriding the ascent value.
     */
    private class Reference<T>(
        private val delegate: BitmapProvider<T>,
        override val ascent: Int
    ) : BitmapProvider<T>() {
        
        override val file: ResourcePath
            get() = delegate.file
        override val height: Int
            get() = delegate.height
        override val glyphImageType
            get() = delegate.glyphImageType
        override val codePointGrid: CodePointGrid
            get() = delegate.codePointGrid
        override val glyphGrid: GlyphGrid<T>
            get() = delegate.glyphGrid
        
        override val charSizes by lazy(::calculateCharSizes)
        
        override fun write(assetsDir: Path) = Unit
        
        override fun toJson() = delegate.toJson().apply {
            addProperty("ascent", ascent)
        }
        
        private fun calculateCharSizes(): Int2ObjectMap<FloatArray> {
            val ascent = ascent
            val ascentDiff = delegate.ascent - ascent
            
            val map = Int2ObjectOpenHashMap<FloatArray>(delegate.charSizes.size)
            for (entry in delegate.charSizes.int2ObjectEntrySet()) {
                val codePoint = entry.intKey
                val sizes = entry.value
                map.put(codePoint, floatArrayOf(
                    sizes[0], // width
                    sizes[1] + ascentDiff, // yMin
                    sizes[2] + ascentDiff  // yMax
                ))
            }
            
            return map
        }
        
    }
    
    /**
     * An immutable [BitmapProvider] that contains only one [BufferedImage] glyph.
     */
    private class SingleBufferedImage(
        override val file: ResourcePath,
        texture: BufferedImage,
        codePoint: Int,
        override val height: Int,
        override val ascent: Int,
    ) : BitmapProvider<BufferedImage>() {
        
        override val glyphImageType = BitmapGlyphImageType.BUFFERED_IMAGE
        
        override val codePointGrid = CodePointGrid.single(codePoint)
        override val glyphGrid = GlyphGrid.single(texture)
        
    }
    
    /**
     * An immutable [BitmapProvider] that lazily loads a single [BufferedImage] glyph.
     */
    private class SingleLazyLoaded(
        override val file: ResourcePath,
        codePoint: Int,
        override val height: Int,
        override val ascent: Int,
    ) : BitmapProvider<BufferedImage>() {
        
        override val glyphImageType = BitmapGlyphImageType.BUFFERED_IMAGE
        
        override val codePointGrid = CodePointGrid.single(codePoint)
        override val glyphGrid by lazy { GlyphGrid.single(file.findInAssets("textures").readImage()) }
        
        // not necessary because the image is loaded from disk and cannot be changed
        override fun write(assetsDir: Path) = Unit
        
    }
    
    /**
     * A custom immutable [BitmapProvider].
     */
    private class Custom<T>(
        override val glyphImageType: BitmapGlyphImageType<T>,
        override val file: ResourcePath,
        override val codePointGrid: CodePointGrid,
        override val glyphGrid: GlyphGrid<T>,
        override val height: Int,
        override val ascent: Int
    ) : BitmapProvider<T>()
    
    companion object {
        
        /**
         * Creates a new immutable [BitmapProvider] with custom grids.
         * 
         * @param file The path to where the texture will be saved.
         * @param codePointGrid The code points, aligned in a grid pointing to the glyphs.
         * @param glyphGrid The glyphs, aligned in a grid pointing to the code points.
         * @param height The height that glyphs will be rendered as. If this does not match the height of the glyphs,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun custom(file: ResourcePath, codePointGrid: CodePointGrid, glyphGrid: GlyphGrid<BufferedImage>, height: Int, ascent: Int): BitmapProvider<BufferedImage> =
            Custom(BitmapGlyphImageType.BUFFERED_IMAGE, file, codePointGrid, glyphGrid, height, ascent)
        
        /**
         * Creates a new immutable [BitmapProvider] with custom grids.
         *
         * @param file The path to where the texture will be saved.
         * @param codePointGrid The code points, aligned in a grid pointing to the glyphs.
         * @param glyphGrid The glyphs, aligned in a grid pointing to the code points.
         * @param height The height that glyphs will be rendered as. If this does not match the height of the glyphs,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        @JvmName("custom1")
        fun custom(file: ResourcePath, codePointGrid: CodePointGrid, glyphGrid: GlyphGrid<IntArray>, height: Int, ascent: Int): BitmapProvider<IntArray> =
            Custom(BitmapGlyphImageType.ARGB_ARRAY, file, codePointGrid, glyphGrid, height, ascent)
        
        /**
         * Creates a new immutable [BitmapProvider] that references another one, but with a different ascent.
         *
         * This is not a reference provider. In the resource pack, this will be a normal bitmap provider.
         */
        fun <T> reference(provider: BitmapProvider<T>, customAscent: Int): BitmapProvider<T> =
            Reference(provider, customAscent)
        
        /**
         * Creates a new immutable [BitmapProvider] with a single glyph from a texture.
         *
         * @param file The path to where the texture will be saved.
         * @param texture The texture containing the glyph.
         * @param codePoint The code point of the glyph.
         * @param height The height that glyphs will be rendered as. If this does not match the height of the glyphs,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun single(file: ResourcePath, texture: BufferedImage, codePoint: Int, height: Int, ascent: Int): BitmapProvider<BufferedImage> =
            SingleBufferedImage(file, texture, codePoint, height, ascent)
        
        /**
         * Creates an immutable [BitmapProvider] with a single glyph, assuming the texture will exist.
         *
         * @param file The path where the texture will be read from and saved to.
         * @param codePoint The code point of the glyph.
         * @param height The height that glyphs will be rendered as. If this does not match the glyphs actual height,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun single(file: ResourcePath, codePoint: Int, height: Int, ascent: Int): BitmapProvider<BufferedImage> =
            SingleLazyLoaded(file, codePoint, height, ascent)
        
    }
    
}