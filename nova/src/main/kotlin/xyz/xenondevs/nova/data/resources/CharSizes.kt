@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.component.adventure.chars
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ComponentSize(
    val width: Int,
    // TODO: xRange
    val yRange: IntRange,
)

data class CharOptions(
    val char: Char,
    val font: String,
    val isBold: Boolean
)

private val LOAD_CHAR_SIZES_ON_STARTUP by configReloadable { DEFAULT_CONFIG.getBoolean("performance.load_char_sizes_on_startup") }

@InternalInit(
    stage = InitializationStage.POST_WORLD_ASYNC,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
object CharSizes {
    
    private val CHAR_SIZES_DIR = File(NOVA.dataFolder, ".internal_data/char_sizes/")
    private val loadedTables = HashMap<String, CharSizeTable>()
    private val componentCache: Cache<Pair<Component, String>, ComponentSize> = CacheBuilder.newBuilder()
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
    
    fun getCharYRange(font: String, char: Int): IntRange =
        getTable(font)?.getYRange(char) ?: IntRange.EMPTY
    
    fun getCharYRange(font: String, char: Char): IntRange =
        getCharYRange(font, char.code)
    
    /**
     * Calculates the width of [string] when rendered with [font].
     */
    fun calculateStringWidth(font: String, string: String): Int {
        return string.toCharArray().sumOf { getCharWidth(font, it) }
    }
    
    /**
     * Calculates the width of a component.
     */
    fun calculateComponentWidth(component: Component, lang: String = "en_us"): Int {
        return calculateComponentSize(component, lang).width
    }
    
    /**
     * Calculates the [ComponentSize] of the given [component] under [lang].
     */
    fun calculateComponentSize(component: Component, lang: String): ComponentSize {
        return componentCache.get(component to lang) {
            calculateComponentSize(
                component.chars(lang)
                    .map {
                        CharOptions(
                            it.char,
                            it.style.font()?.asString() ?: "minecraft:default",
                            it.style.hasDecoration(TextDecoration.BOLD)
                        )
                    }
            )
        }
    }
    
    /**
     * Calculates the [ComponentSize] from a sequence of [CharOptions].
     */
    fun calculateComponentSize(chars: Sequence<CharOptions>): ComponentSize {
        var width = 0
        
        var yRangeMin = Integer.MAX_VALUE
        var yRangeMax = Integer.MIN_VALUE
        
        chars.forEach { (char, font, isBold) ->
            // x
            var charWidth = getCharWidth(font, char)
            if (charWidth < 0) charWidth += 1
            if (isBold) charWidth += 1
            
            width += charWidth
            
            // ignore move font for yRange
            if (font == "nova:move")
                return@forEach
            
            // y
            val yRange = getCharYRange(font, char)
            val charMinY = yRange.first
            val charMaxY = yRange.last
            
            if (charMinY < yRangeMin) yRangeMin = charMinY
            if (charMaxY > yRangeMax) yRangeMax = charMaxY
        }
        
        return ComponentSize(width, yRangeMin..yRangeMax)
    }
    
    @InitFun
    private fun init() {
        if (LOAD_CHAR_SIZES_ON_STARTUP) {
            val service = Executors.newCachedThreadPool()
            
            CHAR_SIZES_DIR.walkTopDown().filter(File::isFile).forEach {
                val fontName = getFontName(it)
                if (fontName !in loadedTables) {
                    service.submit {
                        val table = CharSizeTable.load(it)
                        loadedTables[getFontName(it)] = table
                    }
                }
            }
            
            service.shutdown()
            service.awaitTermination(5, TimeUnit.MINUTES)
        }
    }
    
    internal fun invalidateCache() {
        componentCache.invalidateAll()
    }
    
    private fun getFontName(file: File): String {
        val fontNameParts = file.relativeTo(CHAR_SIZES_DIR).invariantSeparatorsPath
            .substringBeforeLast('.')
            .split('/')
        return "${fontNameParts[0]}:" + fontNameParts.drop(1).joinToString("/")
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
    
    internal fun deleteTable(font: String) {
        loadedTables -= font
        getFile(font).delete()
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
    
    fun getYRange(char: Int): IntRange {
        return sizes[char]?.let { it[3]..it[4] } ?: IntRange.EMPTY
    }
    
    fun setWidth(char: Int, width: Int) {
        sizes.getOrPut(char) { IntArray(5) }[0] = width
    }
    
    fun setHeight(char: Int, height: Int) {
        sizes.getOrPut(char) { IntArray(5) }[1] = height
    }
    
    fun setAscent(char: Int, ascent: Int) {
        sizes.getOrPut(char) { IntArray(5) }[2] = ascent
    }
    
    fun setYRange(char: Int, start: Int, end: Int) {
        val sizes = sizes.getOrPut(char) { IntArray(5) }
        sizes[3] = start
        sizes[4] = end
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
                dout.writeInt(charSizes[3]) // yMin
                dout.writeInt(charSizes[4]) // yMax
            }
        }
    }
    
    companion object {
        
        fun load(file: File): CharSizeTable {
            file.inputStream().use {
                val bytes = it.readAllBytes()
                val din = DataInputStream(ByteArrayInputStream(bytes))
                val sizes = Int2ObjectOpenHashMap<IntArray>()
                
                while (din.available() >= 16) {
                    // char code, width, height, ascent, yMin, yMax
                    sizes[din.readInt()] = intArrayOf(din.readInt(), din.readInt(), din.readInt(), din.readInt(), din.readInt())
                }
                
                return CharSizeTable(sizes)
            }
        }
        
    }
    
}