package xyz.xenondevs.nova.resources.builder.font.provider.bitmap

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet

/**
 * Represents a grid of code points.
 */
sealed interface CodePointGrid {
    
    /**
     * The width of this grid, i.e. the number of columns.
     */
    val width: Int
    
    /**
     * The height of this grid, i.e. the number of rows.
     */
    val height: Int
    
    /**
     * Gets the code point at the specified [x] and [y] coordinates.
     * @throws IndexOutOfBoundsException If the coordinates are out of bounds.
     */
    operator fun get(x: Int, y: Int): Int
    
    /**
     * Creates an [IntSet] of all code points in the grid, excluding 0 (null character).
     */
    fun getCodePoints(): IntSet
    
    /**
     * Creates a list of strings representing the rows of this grid.
     */
    fun toStringList(): List<String>
    
    /**
     * An immutable [CodePointGrid] with only one code point.
     */
    private class Single(val codePoint: Int) : CodePointGrid {
        
        override val width = 1
        override val height = 1
        private val codePoints = IntSet.of(codePoint)
        private val stringList = listOf(Character.toString(codePoint))
        
        override fun get(x: Int, y: Int): Int {
            if (x != 0 || y != 0)
                throw IndexOutOfBoundsException("Coordinates ($x, $y) out of bounds for grid of size (1, 1)")
            
            return codePoint
        }
        
        override fun getCodePoints(): IntSet = codePoints
        override fun toStringList(): List<String> = stringList
        
    }
    
    companion object {
        
        fun single(codePoint: Int): CodePointGrid =
            Single(codePoint)
        
    }
    
}

/**
 * Represents a mutable grid of code points.
 */
sealed interface MutableCodePointGrid : CodePointGrid {
    
    /**
     * The width of this grid, i.e. the number of columns.
     *
     * * If value is decreased, existing columns will be truncated.
     * * If value is increased, new columns will be added and filled with 0 (null character).
     */
    override var width: Int
    
    /**
     * The height of this grid, i.e. the number of rows.
     *
     * * If value is decreased, existing rows will be truncated.
     * * If value is increased, new rows will be added and filled with 0 (null character).
     */
    override var height: Int
    
    /**
     * Sets the code point at the specified [x] and [y] coordinates and resizes the grid if necessary.
     */
    operator fun set(x: Int, y: Int, value: Int)
    
}

/**
 * Represents a grid of code points.
 *
 * This grid internally uses a 2D array, where the first dimension represents the y coordinate (row)
 * and the second dimension represents the x coordinate (column). This unconventional order is used
 * intentionally to improve serialization performance from and to string lists.
 */
class ArrayCodePointGrid(private var grid: Array<IntArray>) : MutableCodePointGrid {
    
    constructor(width: Int, height: Int) : this(Array(height) { IntArray(width) })
    
    init {
        // Validate that all rows have the same size
        var size = -1
        for (row in grid) {
            if (size == -1) {
                size = row.size
                continue
            }
            
            if (row.size != size)
                throw IllegalArgumentException("Row size mismatch")
        }
    }
    
    override var width = grid.getOrNull(0)?.size ?: 0
        set(value) {
            field = value
            
            for (i in grid.indices)
                grid[i] = grid[i].copyOf(value)
        }
    
    override var height = grid.size
        set(value) {
            if (field == value)
                return
            field = value
            
            val newGrid = grid.copyOf(value)
            for (i in grid.size..<newGrid.size)
                newGrid[i] = IntArray(height)
        }
    
    
    override operator fun get(x: Int, y: Int): Int {
        return grid[y][x]
    }
    
    override operator fun set(x: Int, y: Int, value: Int) {
        // resize grid if necessary
        if (width <= x)
            width = x + 1
        if (height <= y)
            height = y + 1
        
        grid[y][x] = value
    }
    
    override fun getCodePoints(): IntSet {
        val set = IntOpenHashSet()
        for (row in grid) {
            for (codePoint in row) {
                if (codePoint != 0) {
                    set.add(codePoint)
                }
            }
        }
        return set
    }
    
    override fun toStringList(): List<String> {
        val rows = ArrayList<String>()
        for (y in 0..<height) {
            val builder = StringBuilder()
            for (x in 0..<width) {
                builder.appendCodePoint(grid[y][x])
            }
            rows += builder.toString()
        }
        
        return rows
    }
    
    companion object {
        
        /**
         * Creates a [CodePointGrid] from a list of strings.
         */
        @Suppress("UNCHECKED_CAST")
        fun of(rows: List<String>): ArrayCodePointGrid {
            val array: Array<IntArray?> = arrayOfNulls(rows.size)
            for (i in rows.indices) {
                array[i] = rows[i].codePoints().toArray()
            }
            
            return ArrayCodePointGrid(array as Array<IntArray>)
        }
        
    }
    
}