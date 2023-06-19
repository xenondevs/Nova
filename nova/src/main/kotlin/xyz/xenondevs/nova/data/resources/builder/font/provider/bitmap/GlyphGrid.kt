package xyz.xenondevs.nova.data.resources.builder.font.provider.bitmap

import xyz.xenondevs.nova.util.data.ImageUtils
import java.awt.image.BufferedImage

sealed interface GlyphGrid<T> {
    
    /**
     * The width of this grid, i.e. the number of columns or the number of glyphs per row.
     */
    val gridWidth: Int
    
    /**
     * The height of this grid, i.e. the number of rows.
     */
    val gridHeight: Int
    
    /**
     * The width of each glyph.
     */
    val glyphWidth: Int
    
    /**
     * The height of each glyph
     */
    val glyphHeight: Int
    
    /**
     * Gets the glyph at the specified [x] and [y] coordinates.
     * 
     * @throws IndexOutOfBoundsException If the coordinates are out of bounds.
     * @throws NoSuchElementException If there is no glyph at the specified coordinates.
     */
    operator fun get(x: Int, y: Int): T
    
    /**
     * Creates a [BufferedImage] of this grid.
     */
    fun toImage(): BufferedImage
    
    /**
     * An immutable [GlyphGrid] with only one [BufferedImage] glyph.
     */
    private class SingleBufferedImage(private val image: BufferedImage) : GlyphGrid<BufferedImage> {
        
        override val glyphWidth: Int
            get() = image.width
        override val glyphHeight: Int
            get() = image.height
        override val gridWidth = 1
        override val gridHeight = 1
        
        override fun get(x: Int, y: Int): BufferedImage {
            if (x != 0 || y != 0)
                throw IndexOutOfBoundsException("Coordinates ($x, $y) out of bounds for grid of size (1, 1)")
            
            return image
        }
        
        override fun toImage(): BufferedImage {
            return image
        }
        
    }
    
    companion object {
        
        /**
         * Creates a new immutable [GlyphGrid] that holds a single [BufferedImage] at (0, 0).
         */
        fun single(image: BufferedImage): GlyphGrid<BufferedImage> =
            SingleBufferedImage(image)
        
    }
    
}

sealed interface MutableGlyphGrid<T> : GlyphGrid<T> {
    
    /**
     * The width of this grid, i.e. the number of columns.
     *
     * * If value is decreased, existing columns will be truncated.
     * * If value is increased, new columns will be added and filled with 0 (null character).
     */
    override var gridWidth: Int
    
    /**
     * The height of this grid, i.e. the number of rows.
     *
     * * If value is decreased, existing rows will be truncated.
     * * If value is increased, new rows will be added and filled with 0 (null character).
     */
    override var gridHeight: Int
    
    /**
     * Sets the glyph at the specified [x] and [y] coordinates, or removes it if [value] is null.
     * 
     * If the coordinates are out of bounds, the grid will be resized to fit the coordinates.
     */
    operator fun set(x: Int, y: Int, value: T?)
    
    /**
     * Creates a new glyph at the specified [x] and [y] and returns it.
     * Changes made to the returned glyph will be reflected in this grid.
     * 
     * If the coordinates are out of bounds, the grid will be resized to fit the coordinates.
     */
    fun create(x: Int, y: Int): T
    
}

/**
 * An immutable [GlyphGrid] implementation that stores the entire grid in an argb [raster][IntArray].
 * 
 * @param glyphWidth The width of each glyph.
 * @param glyphHeight The height of each glyph.
 * @param gridWidth The width of the grid, i.e. the number of columns.
 * @param gridHeight The height of the grid, i.e. the number of rows.
 * @param raster The backing raster.
 */
class RasterGlyphGrid(
    override val gridWidth: Int,
    override val gridHeight: Int,
    override val glyphWidth: Int,
    override val glyphHeight: Int,
    private var raster: IntArray
) : GlyphGrid<IntArray> {
    
    private val glyphSize = glyphWidth * glyphHeight
    
    init {
        require(raster.size == gridWidth * glyphWidth * gridHeight * glyphHeight) { "Raster size does not match other parameters" }
        require(glyphWidth in 0..256 && glyphHeight in 0..256) { "Max glyph size is 256x256" }
    }
    
    override fun get(x: Int, y: Int): IntArray {
        if (x !in 0..gridWidth || y !in 0..gridHeight)
            throw IndexOutOfBoundsException("Coordinates ($x, $y) are out of bounds for grid ($gridWidth x $gridHeight)")
        
        val i = y * glyphSize
        return raster.copyOfRange(i, i + glyphSize)
    }
    
    override fun toImage(): BufferedImage {
        return ImageUtils.createImageFromArgbRaster(gridWidth * glyphWidth , raster)
    }
    
}

/**
 * A mutable [GlyphGrid] implementation that just stores references to [BufferedImages][BufferedImage].
 */
class ReferenceGlyphGrid(
    private var grid: Array<Array<BufferedImage?>>,
    override val glyphWidth: Int,
    override val glyphHeight: Int
) : MutableGlyphGrid<BufferedImage> {
    
    constructor(gridWidth: Int, gridHeight: Int, glyphWidth: Int, glyphHeight: Int) : this(
        Array(gridWidth) { arrayOfNulls(gridHeight) },
        glyphWidth, glyphHeight
    )
    
    init {
        require(glyphWidth in 0..256 && glyphHeight in 0..256) { "Max glyph size is 256x256 (got $" }
    }
    
    override var gridWidth = grid.size
        set(value) {
            if (field == value)
                return
            field = value
            
            val newGrid = grid.copyOf(value)
            for (i in grid.size until newGrid.size)
                newGrid[i] = arrayOfNulls(gridHeight)
        }
    
    override var gridHeight = grid.getOrNull(0)?.size ?: 0
        set(value) {
            field = value
            
            for (i in grid.indices)
                grid[i] = grid[i].copyOf(value)
        }
    
    override operator fun get(x: Int, y: Int): BufferedImage {
        return grid[x][y] ?: throw NoSuchElementException("No glyph at ($x, $y)")
    }
    
    override operator fun set(x: Int, y: Int, value: BufferedImage?) {
        // resize grid if necessary
        if (gridWidth <= x)
            gridWidth = x + 1
        if (gridHeight <= y)
            gridHeight = y + 1
        
        grid[x][y] = value
    }
    
    override fun create(x: Int, y: Int): BufferedImage {
        val img = BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB)
        set(x, y, img)
        return img
    }
    
    override fun toImage(): BufferedImage {
        val image = BufferedImage(gridWidth * glyphWidth, gridHeight * glyphHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.graphics
        
        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                val glyph = grid[x][y] ?: continue
                
                if (glyph.width != glyphWidth || glyph.height != glyphHeight)
                    throw IllegalArgumentException("Glyph at ($x, $y) has invalid dimensions: Expected ($glyphWidth, $glyphHeight), got (${glyph.width}, ${glyph.height})")
                
                graphics.drawImage(glyph, x * glyphWidth, y * glyphHeight, null)
            }
        }
        
        graphics.dispose()
        return image
    }
    
    companion object {
        
        fun of(img: BufferedImage, glyphWidth: Int, glyphHeight: Int): ReferenceGlyphGrid {
            require(img.width % glyphWidth == 0 && img.height % glyphHeight == 0) {
                "Image width and height must be a multiple of the glyph width and height"
            }
            
            val columns = img.width / glyphWidth
            val rows = img.height / glyphHeight
            
            return ReferenceGlyphGrid(
                Array(columns) { x -> Array(rows) { y -> img.getSubimage(x * glyphWidth, y * glyphHeight, glyphWidth, glyphHeight) } },
                glyphWidth, glyphHeight
            )
        }
        
    }
    
}