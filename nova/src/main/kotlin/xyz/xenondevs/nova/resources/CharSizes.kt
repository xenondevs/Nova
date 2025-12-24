@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.resources

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.component.adventure.StyledElement
import xyz.xenondevs.nova.util.component.adventure.elements
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeBytes

private val LOAD_CHAR_SIZES_ON_STARTUP by MAIN_CONFIG.entry<Boolean>("performance", "load_char_sizes_on_startup")

private val EMPTY_FLOAT_RANGE: ClosedRange<Float> = 0f..0f

data class ComponentSize(
    val width: Float,
    // TODO: xRange
    val yRange: ClosedRange<Float>,
)

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
object CharSizes {
    
    private val CHAR_SIZES_DIR = DATA_FOLDER.resolve(".internal_data/char_sizes/")
    private val loadedTables = HashMap<Key, CharSizeTable>()
    private val componentCache: Cache<Pair<Component, String>, ComponentSize> = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build()
    
    /**
     * Gets the width of [char] when rendered with [font].
     *
     * Note: This width includes the one pixel space rendered between characters.
     */
    fun getCharWidth(font: Key, char: Int): Float =
        getTable(font)?.getWidth(char) ?: 0f
    
    /**
     * Gets the width of [char] when rendered with [font].
     *
     * Note: This width includes the one pixel space rendered between characters.
     */
    fun getCharWidth(font: Key, char: Char): Float =
        getCharWidth(font, char.code)
    
    fun getCharYRange(font: Key, char: Int): ClosedRange<Float> =
        getTable(font)?.getYRange(char) ?: EMPTY_FLOAT_RANGE
    
    fun getCharYRange(font: Key, char: Char): ClosedRange<Float> =
        getCharYRange(font, char.code)
    
    /**
     * Calculates the width of [string] when rendered with [font].
     */
    fun calculateStringWidth(font: Key, string: String): Float {
        val table = getTable(font) ?: return 0f
        return string.codePoints().mapToDouble { table.getWidth(it).toDouble() }.sum().toFloat()
    }
    
    /**
     * Calculates the width of a component.
     */
    fun calculateComponentWidth(component: Component, lang: String = "en_us"): Float {
        return calculateComponentSize(component, lang).width
    }
    
    /**
     * Calculates the [ComponentSize] of the given [component] under [lang].
     */
    fun calculateComponentSize(component: Component, lang: String): ComponentSize {
        return componentCache.get(component to lang) { calculateComponentSize(component.elements(lang)) }
    }
    
    /**
     * Calculates the [ComponentSize] from a sequence of [StyledElements][StyledElement].
     */
    fun calculateComponentSize(elements: Sequence<StyledElement>): ComponentSize {
        var width = 0f
        
        var yRangeMin = Float.MAX_VALUE
        var yRangeMax = -Float.MAX_VALUE
        
        for (element in elements) {
            if (element is StyledElement.CodePoint) {
                val fontKey = element.style.font() ?: Key.key("minecraft", "default")
                
                // x
                var charWidth = getCharWidth(fontKey, element.codePoint)
                if (element.style.hasDecoration(TextDecoration.BOLD)) charWidth += 1
                width += charWidth
                
                // y
                val yRange = getCharYRange(fontKey, element.codePoint)
                val charMinY = yRange.start
                val charMaxY = yRange.endInclusive
                
                if (charMinY < yRangeMin) yRangeMin = charMinY
                if (charMaxY > yRangeMax) yRangeMax = charMaxY
            } else if (element is StyledElement.Object) {
                width += 8
                if (-8f < yRangeMin) yRangeMin = -8f
                if (-1f > yRangeMax) yRangeMax = -1f
            }
        }
        
        return ComponentSize(width, yRangeMin..yRangeMax)
    }
    
    @InitFun
    private fun init() {
        if (LOAD_CHAR_SIZES_ON_STARTUP) {
            val service = Executors.newCachedThreadPool()
            
            CHAR_SIZES_DIR.walk().filter(Path::isRegularFile).forEach {
                val fontKey = getFontKey(it)
                if (fontKey !in loadedTables) {
                    service.submit {
                        val table = CharSizeTable.load(it)
                        loadedTables[getFontKey(it)] = table
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
    
    private fun getFontKey(file: Path): Key {
        val fontNameParts = file.relativeTo(CHAR_SIZES_DIR).invariantSeparatorsPathString
            .substringBeforeLast('.')
            .split('/')
        return Key.key(fontNameParts[0], fontNameParts.drop(1).joinToString("/"))
    }
    
    private fun loadTable(font: Key): CharSizeTable? {
        val file = getFile(font)
        if (file.exists()) {
            val table = CharSizeTable.load(file)
            loadedTables[font] = table
            return table
        }
        
        return null
    }
    
    internal fun storeTable(font: Key, table: CharSizeTable) {
        loadedTables[font] = table
        table.write(getFile(font))
    }
    
    internal fun getTable(font: Key): CharSizeTable? {
        return loadedTables[font] ?: loadTable(font)
    }
    
    internal fun deleteTable(font: Key) {
        loadedTables -= font
        getFile(font).deleteIfExists()
    }
    
    private fun getFile(font: Key): Path {
        val file = CHAR_SIZES_DIR.resolve("${font.namespace()}/${font.value()}.bin")
        file.parent.createDirectories()
        return file
    }
    
}

internal class CharSizeTable(
    private val sizes: Int2ObjectMap<FloatArray> = Int2ObjectOpenHashMap(),
) {
    
    fun getWidth(char: Int): Float {
        return sizes[char]?.get(0) ?: 0f
    }
    
    fun getYRange(char: Int): ClosedRange<Float> {
        return sizes[char]?.let { it[1]..it[2] } ?: 0f..0f
    }
    
    fun setWidth(char: Int, width: Float) {
        sizes.getOrPut(char) { FloatArray(3) }[0] = width
    }
    
    fun setYRange(char: Int, start: Float, end: Float) {
        val sizes = sizes.getOrPut(char) { FloatArray(3) }
        sizes[1] = start
        sizes[2] = end
    }
    
    fun setSizes(char: Int, sizes: FloatArray) {
        this.sizes[char] = sizes
    }
    
    /**
     * Merges [other] into this [CharSizeTable], overriding existing values.
     */
    fun merge(other: Int2ObjectMap<FloatArray>) {
        sizes.putAll(other)
    }
    
    /**
     * Merges [other] into this [CharSizeTable], overriding existing values.
     */
    fun merge(other: CharSizeTable) {
        sizes.putAll(other.sizes)
    }
    
    operator fun contains(char: Int): Boolean {
        return char in sizes
    }
    
    fun write(file: Path) {
        val buf = ByteBuffer.allocate(sizes.size * 16) // 1 int, 3 floats, 4 bytes each
        for (entry in sizes.int2ObjectEntrySet()) {
            val codePoint = entry.intKey
            val charSizes = entry.value
            
            buf.putInt(codePoint)
            buf.putFloat(charSizes[0]) // width
            buf.putFloat(charSizes[1]) // yMin
            buf.putFloat(charSizes[2]) // yMax
        }
        
        file.writeBytes(buf.array())
    }
    
    companion object {
        
        fun load(file: Path): CharSizeTable {
            val bytes = file.readBytes()
            val din = DataInputStream(ByteArrayInputStream(bytes))
            val sizes = Int2ObjectOpenHashMap<FloatArray>()
            
            while (din.available() >= 16) {
                // char code, width, yMin, yMax
                sizes[din.readInt()] = floatArrayOf(din.readFloat(), din.readFloat(), din.readFloat())
            }
            
            return CharSizeTable(sizes)
        }
        
    }
    
}