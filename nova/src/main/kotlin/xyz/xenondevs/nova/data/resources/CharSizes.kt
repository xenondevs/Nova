@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.component.adventure.chars
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val LOAD_CHAR_SIZES_ON_STARTUP by MAIN_CONFIG.entry<Boolean>("performance", "load_char_sizes_on_startup")
private val FORCE_UNIFORM_FONT by MAIN_CONFIG.entry<Boolean>("resource_pack", "force_uniform_font")

private val EMPTY_FLOAT_RANGE: ClosedRange<Float> = 0f..0f

data class ComponentSize(
    val width: Float,
    // TODO: xRange
    val yRange: ClosedRange<Float>,
)

data class CharOptions(
    val char: Char,
    val font: String,
    val isBold: Boolean
)

@InternalInit(
    stage = InternalInitStage.POST_WORLD_ASYNC,
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
    fun getCharWidth(font: String, char: Int): Float =
        getTable(font)?.getWidth(char) ?: 0f
    
    /**
     * Gets the width of [char] when rendered with [font].
     *
     * Note: This width includes the one pixel space rendered between characters.
     */
    fun getCharWidth(font: String, char: Char): Float =
        getCharWidth(font, char.code)
    
    fun getCharYRange(font: String, char: Int): ClosedRange<Float> =
        getTable(font)?.getYRange(char) ?: EMPTY_FLOAT_RANGE
    
    fun getCharYRange(font: String, char: Char): ClosedRange<Float> =
        getCharYRange(font, char.code)
    
    /**
     * Calculates the width of [string] when rendered with [font].
     */
    fun calculateStringWidth(font: String, string: String): Float {
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
        var width = 0f
        
        var yRangeMin = Float.MAX_VALUE
        var yRangeMax = Float.MIN_VALUE
        
        chars.forEach { (char, font, isBold) ->
            // x
            var charWidth = getCharWidth(font, char)
            if (isBold) charWidth += 1
            width += charWidth
            
            // y
            val yRange = getCharYRange(font, char)
            val charMinY = yRange.start
            val charMaxY = yRange.endInclusive
            
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
    
    internal fun storeTable(font: ResourcePath, table: CharSizeTable) =
        storeTable(font.toString(), table)
    
    internal fun storeTable(font: String, table: CharSizeTable) {
        loadedTables[font] = table
        table.write(getFile(font))
    }
    
    internal fun getTable(font: ResourcePath) =
        getTable(font.toString())
    
    internal fun getTable(font: String): CharSizeTable? {
        var namespacedFont = if (font.contains(':')) font else "minecraft:$font"
        
        if (FORCE_UNIFORM_FONT && namespacedFont == "minecraft:default")
            namespacedFont = "minecraft:uniform"
            
        return loadedTables[namespacedFont] ?: loadTable(namespacedFont)
    }
    
    internal fun deleteTable(font: ResourcePath) =
        deleteTable(font.toString())
    
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
    
    fun write(file: File) {
        val buf = ByteBuffer.allocate(sizes.size * 16) // 1 int, 3 floats, 4 bytes each
        for(entry in sizes.int2ObjectEntrySet()) {
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
        
        fun load(file: File): CharSizeTable {
            file.inputStream().use {
                val bytes = it.readAllBytes()
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
    
}