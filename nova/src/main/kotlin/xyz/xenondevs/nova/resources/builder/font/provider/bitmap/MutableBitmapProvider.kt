package xyz.xenondevs.nova.resources.builder.font.provider.bitmap

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getAllStrings
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getInt
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.toJsonArray
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.task.TextureContent
import xyz.xenondevs.nova.serialization.json.getDeserialized
import java.awt.image.BufferedImage

abstract class MutableBitmapProvider<T> : BitmapProvider<T>() {
    
    // -- Mutable properties --
    abstract override var file: ResourcePath<ResourceType.FontTexture>
    abstract override var height: Int
    abstract override var ascent: Int
    abstract override val codePointGrid: MutableCodePointGrid
    abstract override val glyphGrid: MutableGlyphGrid<T>
    
    /**
     * Whether glyphs have been updated.
     */
    private var glyphsChanged = false
    
    /**
     * Whether code points have been updated.
     */
    protected var codePointsChanged = false
    
    /**
     * Puts the given [codePoint] and [glyph] into the grid at [x] and [y].
     */
    fun set(x: Int, y: Int, codePoint: Int, glyph: T) {
        set(x, y, codePoint)
        set(x, y, glyph)
    }
    
    /**
     * Puts the given [codePoint] into the grid at [x] and [y].
     */
    operator fun set(x: Int, y: Int, codePoint: Int) {
        codePointGrid[x, y] = codePoint
        codePointsChanged = true
    }
    
    /**
     * Puts the given [glyph] into the grid at [x] and [y].
     */
    operator fun set(x: Int, y: Int, glyph: T) {
        glyphGrid[x, y] = glyph
        glyphsChanged = true
    }
    
    /**
     * Creates a new glyph texture puts it into the grid at [x] and [y].
     */
    fun createGlyph(x: Int, y: Int, codePoint: Int): T {
        glyphsChanged = true
        codePointsChanged = true
        codePointGrid[x, y] = codePoint
        return glyphGrid.create(x, y)
    }
    
    override fun write(builder: ResourcePackBuilder) {
        if (glyphsChanged) {
            super.write(builder)
        }
    }
    
    /**
     * A [MutableBitmapProvider] that is read lazily.
     */
    private class LazyLoaded(
        private val builder: ResourcePackBuilder,
        private val codePointRows: List<String>,
        override var file: ResourcePath<ResourceType.FontTexture>,
        override var height: Int,
        override var ascent: Int
    ) : MutableBitmapProvider<BufferedImage>() {
        
        override val glyphImageType = BitmapGlyphImageType.BUFFERED_IMAGE
        
        override val codePointGrid: ArrayCodePointGrid by lazy { ArrayCodePointGrid.of(codePointRows) }
        override val glyphGrid: ReferenceGlyphGrid by lazy(::loadGlyphGrid)
        
        private fun loadGlyphGrid(): ReferenceGlyphGrid {
            val img = builder.getHolder<TextureContent>().getImage(file)
            val codePoints = codePointGrid
            
            require(img.width % codePoints.width == 0 && img.height % codePoints.height == 0) {
                "The image size (${img.width}x${img.height}) is not a multiple of the grid size (${codePoints.width}x${codePoints.height})"
            }
            
            val glyphWidth = img.width / codePoints.width
            val glyphHeight = img.height / codePoints.height
            return ReferenceGlyphGrid.of(img, glyphWidth, glyphHeight)
        }
        
        override fun toJson() = super.toJson().apply {
            addProperty("file", file.toString())
            addProperty("height", height)
            addProperty("ascent", ascent)
            val rows = if (codePointsChanged) codePointGrid.toStringList() else codePointRows
            add("chars", rows.toJsonArray())
        }
        
    }
    
    /**
     * A [MutableBitmapProvider] that allows custom grids.
     */
    private class Custom<T>(
        override val glyphImageType: BitmapGlyphImageType<T>,
        override var file: ResourcePath<ResourceType.FontTexture>,
        override val codePointGrid: MutableCodePointGrid,
        override val glyphGrid: MutableGlyphGrid<T>,
        override var height: Int,
        override var ascent: Int
    ) : MutableBitmapProvider<T>()
    
    companion object {
        
        /**
         * Creates a new mutable [BitmapProvider] without any glyphs.
         *
         * @param file The path where the texture will be saved to.
         * @param glyphWidth The width that glyph images will have.
         * @param glyphHeight The height that glyph images will have.
         * @param height The height that glyphs will be rendered as. If this does not match [glyphHeight],
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun create(file: ResourcePath<ResourceType.FontTexture>, glyphWidth: Int, glyphHeight: Int, height: Int, ascent: Int): MutableBitmapProvider<BufferedImage> =
            Custom(
                BitmapGlyphImageType.BUFFERED_IMAGE,
                file,
                ArrayCodePointGrid(0, 0), ReferenceGlyphGrid(0, 0, glyphWidth, glyphHeight),
                height, ascent
            )
        
        /**
         * Creates a new mutable [BitmapProvider] with custom grids.
         *
         * @param file The path where the texture will be saved to.
         * @param codePointGrid The grid containing the code points.
         * @param glyphGrid The grid containing the glyph images.
         * @param height The height that glyphs will be rendered as. If this does not match the glyphs actual height,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun custom(file: ResourcePath<ResourceType.FontTexture>, codePointGrid: MutableCodePointGrid, glyphGrid: MutableGlyphGrid<BufferedImage>, height: Int, ascent: Int): MutableBitmapProvider<BufferedImage> =
            Custom(BitmapGlyphImageType.BUFFERED_IMAGE, file, codePointGrid, glyphGrid, height, ascent)
        
        /**
         * Creates a new mutable [BitmapProvider] with custom grids.
         *
         * @param file The path where the texture will be saved to.
         * @param codePointGrid The grid containing the code points.
         * @param glyphGrid The grid containing the glyph images.
         * @param height The height that glyphs will be rendered as. If this does not match the glyphs actual height,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        @JvmName("custom1")
        fun custom(file: ResourcePath<ResourceType.FontTexture>, codePointGrid: MutableCodePointGrid, glyphGrid: MutableGlyphGrid<IntArray>, height: Int, ascent: Int): MutableBitmapProvider<IntArray> =
            Custom(BitmapGlyphImageType.ARGB_ARRAY, file, codePointGrid, glyphGrid, height, ascent)
        
        /**
         * Creates a new [BitmapProvider] with a single glyph from a texture.
         *
         * @param file The path where the texture will be saved to.
         * @param texture The texture containing the glyph.
         * @param codePoint The code point of the glyph.
         * @param height The height that glyphs will be rendered as. If this does not match glyphs actual height,
         * the glyphs will be scaled, keeping the aspect ratio. This is the `height` property in json.
         * @param ascent The defined character ascent. This is the `ascent` property in json.
         */
        fun single(file: ResourcePath<ResourceType.FontTexture>, texture: BufferedImage, codePoint: Int, height: Int, ascent: Int): MutableBitmapProvider<BufferedImage> {
            val provider = create(file, texture.width, texture.height, height, ascent)
            provider.set(0, 0, codePoint, texture)
            return provider
        }
        
        /**
         * Reads a [BitmapProvider] from disk.
         *
         * @param builder The [ResourcePackBuilder] to use for loading the texture.
         * @param provider The json object containing the provider data. Might be modified by the resulting [BitmapProvider].
         */
        fun fromDisk(builder: ResourcePackBuilder, provider: JsonObject): MutableBitmapProvider<BufferedImage> {
            val file = provider.getDeserialized<ResourcePath<ResourceType.FontTexture>>("file")
            val height = provider.getIntOrNull("height") ?: 8
            val ascent = provider.getInt("ascent")
            return LazyLoaded(builder, provider.getArray("chars").getAllStrings(), file, height, ascent)
        }
        
    }
    
}