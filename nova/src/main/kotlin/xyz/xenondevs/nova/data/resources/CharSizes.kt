@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.ArrayKey
import xyz.xenondevs.nova.util.data.toPlainText
import xyz.xenondevs.nova.util.removeMinecraftFormatting
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ComponentSize(
    val width: Int,
    val xRange: IntRange,
    val yRange: IntRange,
)

object CharSizes {
    
    private val CHAR_SIZES_DIR = File(NOVA.dataFolder, ".internal_data/char_sizes/")
    
    private val loadedTables = HashMap<String, CharSizeTable>()
    
    private val componentSizeCache: Cache<Pair<ArrayKey<BaseComponent>, String>, ComponentSize> = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build()
    
    /**
     * Gets the width of [char] when rendered with [font].
     *
     * Note: This width includes the one pixel space rendered between characters.
     */
    fun getCharWidth(font: String, char: Int): Int =
        getTable(font)?.getWidth(char) ?: 0
    
    /**
     * Gets the width of [char] when rendered with [font].
     *
     * Note: This width includes the one pixel space rendered between characters.
     */
    fun getCharWidth(font: String, char: Char): Int =
        getCharWidth(font, char.code)
    
    /**
     * Gets the width of [char] when rendered with [font].
     */
    fun getCharHeight(font: String, char: Int): Int =
        getTable(font)?.getHeight(char) ?: 0
    
    /**
     * Gets the width of [char] when rendered with [font].
     */
    fun getCharHeight(font: String, char: Char): Int =
        getCharHeight(font, char.code)
    
    /**
     * Gets the ascent of [char] when rendered with [font].
     */
    fun getCharAscent(font: String, char: Int): Int =
        getTable(font)?.getAscent(char) ?: 0
    
    /**
     * Gets the ascent of [char] when rendered with [font].
     */
    fun getCharAscent(font: String, char: Char): Int =
        getCharAscent(font, char.code)
    
    /**
     * Calculates the width of [string] when rendered with [font].
     */
    fun calculateStringWidth(font: String, string: String): Int {
        return string.toCharArray().sumOf { getCharWidth(font, it) }
    }
    
    /**
     * Calculates the width of a component array in pixels.
     */
    fun calculateComponentWidth(components: Array<out BaseComponent>, locale: String): Int {
        return calculateComponentSize(components, locale).width
    }
    
    /**
     * Calculates the [ComponentSize] of the given [components] array under [locale].
     */
    fun calculateComponentSize(components: Array<out BaseComponent>, locale: String): ComponentSize {
        return componentSizeCache.get(ArrayKey(components) to locale) {
            var width = 0
            
            var xRangeMin = 0
            var xRangeMax = 0
            var yRangeMin = 0
            var yRangeMax = 0
            
            for (component in components) {
                val text = component.toPlainText(locale).removeMinecraftFormatting()
                val font = component.font ?: "default"
                for (char in text.toCharArray()) {
                    // x
                    var charWidth = getCharWidth(font, char)
                    if (charWidth < 0) charWidth += 1
                    if (component.isBold) charWidth += 1
                    
                    width += charWidth
                    
                    if (xRangeMin > width) xRangeMin = width
                    if (xRangeMax < width) xRangeMax = width
                    
                    // ignore move font for yRange
                    if (font == "nova:move")
                        continue
                    
                    // y
                    val charHeight = getCharHeight(font, char)
                    val charAscent = getCharAscent(font, char)
                    
                    val charYMin = -charAscent
                    val charYMax = -charAscent + charHeight
                    
                    if (yRangeMin > charYMin) yRangeMin = charYMin
                    if (yRangeMax < charYMax) yRangeMax = charYMax
                }
            }
            
            return@get ComponentSize(width, xRangeMin..xRangeMax, yRangeMin..yRangeMax)
        }
    }
    
    private fun loadTable(font: String): CharSizeTable? {
        val file = getFile(font)
        if (file.exists()) {
            val table = CharSizeTable.load(file)
            loadedTables[font] = table
            return table
        }
        
        return null
    }
    
    internal fun storeTable(font: String, table: CharSizeTable) {
        loadedTables[font] = table
        table.write(getFile(font))
    }
    
    internal fun getTable(font: String): CharSizeTable? {
        val namespacedFont = if (font.contains(':')) font else "minecraft:$font"
        return loadedTables[namespacedFont] ?: loadTable(namespacedFont)
    }
    
    internal fun deleteTables() {
        loadedTables.clear()
        CHAR_SIZES_DIR.deleteRecursively()
    }
    
    private fun getFile(font: String): File {
        val path = ResourcePath.of(font, "minecraft")
        val file = File(CHAR_SIZES_DIR, "${path.namespace}/${path.path}.bin")
        file.parentFile.mkdirs()
        return file
    }
    
}

internal class CharSizeTable(
    private val sizes: MutableMap<Int, IntArray> = Int2ObjectOpenHashMap(),
) {
    
    fun getWidth(char: Int): Int {
        return sizes[char]?.get(0) ?: 0
    }
    
    fun getHeight(char: Int): Int {
        return sizes[char]?.get(1) ?: 0
    }
    
    fun getAscent(char: Int): Int {
        return sizes[char]?.get(2) ?: 0
    }
    
    fun setWidth(char: Int, width: Int) {
        sizes.getOrPut(char) { IntArray(3) }[0] = width
    }
    
    fun setHeight(char: Int, height: Int) {
        sizes.getOrPut(char) { IntArray(3) }[1] = height
    }
    
    fun setAscent(char: Int, ascent: Int) {
        sizes.getOrPut(char) { IntArray(3) }[2] = ascent
    }
    
    fun setSizes(char: Int, sizes: IntArray) {
        this.sizes[char] = sizes
    }
    
    operator fun contains(char: Int): Boolean {
        return char in sizes
    }
    
    fun write(file: File) {
        FileOutputStream(file).use { out ->
            val dout = DataOutputStream(out)
            
            sizes.forEach { (char, charSizes) ->
                dout.writeInt(char)
                dout.writeInt(charSizes[0]) // width
                dout.writeInt(charSizes[1]) // height
                dout.writeInt(charSizes[2]) // ascent
            }
        }
    }
    
    companion object {
        
        fun load(file: File): CharSizeTable {
            LOGGER.info("Loading char size table: $file")
            file.inputStream().use {
                val din = DataInputStream(it)
                val sizes = Int2ObjectOpenHashMap<IntArray>()
                
                while (din.available() >= 16) {
                    // char code, width, height, ascent
                    sizes[din.readInt()] = intArrayOf(din.readInt(), din.readInt(), din.readInt())
                }
                
                return CharSizeTable(sizes)
            }
        }
        
    }
    
}