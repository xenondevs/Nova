@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.resources

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
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
import java.io.BufferedInputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private val LOAD_CHAR_SIZES_ON_STARTUP by MAIN_CONFIG.entry<Boolean>("performance", "load_char_sizes_on_startup")

private val EMPTY_COMPONENT_RANGE: ClosedRange<Float> = 0f..0f
private val NO_VISUAL_RANGE: ClosedRange<Float> = Float.MAX_VALUE..-Float.MAX_VALUE

private const val DIRECT_TABLE_TYPE = 0
private const val REFERENCE_TABLE_TYPE = 1
private const val COMPOSITE_TABLE_TYPE = 2
private const val MOVED_TABLE_TYPE = 3

/**
 * The y-coordinate of Minecraft's text baseline.
 *
 * Nova stores the actual rendered y-positions of characters using the text baseline as y = 0. Minecraft's italic
 * calculation instead uses positions relative to the top of the text line, so the baseline must be added before
 * calculating italic bounds.
 *
 * See [GlyphBitmap](https://mcsrc.dev/2/26.2/com/mojang/blaze3d/font/GlyphBitmap#L24-29).
 */
internal const val FONT_BASELINE = 7f

/**
 * The amount by which Minecraft expands each side of a bold character.
 *
 * This only changes the visible bounds. It does not change the character width or accumulate across characters.
 *
 * See [BakedSheetGlyph](https://mcsrc.dev/2/26.2/net/minecraft/client/gui/font/glyphs/BakedSheetGlyph#L141-143).
 */
private const val BOLD_EXTRA_THICKNESS = 0.1f

/**
 * The size of a rendered [Component].
 */
data class ComponentSize(
    /**
     * How far the next component is moved to the right.
     */
    val width: Float,
    /**
     * The leftmost and rightmost drawn positions, including italic text, bold text, and shadows.
     */
    val xRange: ClosedRange<Float>,
    /**
     * The highest and lowest drawn positions relative to the text baseline, including bold text and shadows.
     */
    val yRange: ClosedRange<Float>,
)

/**
 * Provides generated glyph metrics and calculates logical and visual component dimensions.
 */
@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    runAfter = [ResourceGeneration.PostWorld::class]
)
object CharSizes {
    
    private val CHAR_SIZES_DIR = DATA_FOLDER.resolve(".internal_data/char_sizes/")
    private val loadedTables = ConcurrentHashMap<Key, CharSizeTable>()
    private val componentCache: Cache<Triple<Component, String, Boolean>, ComponentSize> = Caffeine.newBuilder()
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
    
    /**
     * Gets the left and right bounds of code point [char] in [font], before italic, bold, and shadow are applied.
     * Returns an empty range if the character is missing.
     */
    fun getCharXRange(font: Key, char: Int): ClosedRange<Float> =
        getTable(font)?.getXRange(char) ?: NO_VISUAL_RANGE
    
    /**
     * Gets the left and right bounds of [char] in [font], before italic, bold, and shadow are applied.
     * Returns an empty range if the character is missing.
     */
    fun getCharXRange(font: Key, char: Char): ClosedRange<Float> =
        getCharXRange(font, char.code)
    
    /**
     * Gets how far Minecraft moves the extra copy used to draw code point [char] in bold.
     * This is also added to the character width.
     */
    fun getCharBoldOffset(font: Key, char: Int): Float =
        getTable(font)?.getBoldOffset(char) ?: 1f
    
    /**
     * Gets how far Minecraft moves the shadow of code point [char] to the right and down.
     */
    fun getCharShadowOffset(font: Key, char: Int): Float =
        getTable(font)?.getShadowOffset(char) ?: 1f
    
    /**
     * Gets the top and bottom bounds of code point [char] relative to the text baseline, before bold and shadow are applied.
     * Returns an empty range if the character is missing.
     */
    fun getCharYRange(font: Key, char: Int): ClosedRange<Float> =
        getTable(font)?.getYRange(char) ?: NO_VISUAL_RANGE
    
    /**
     * Gets the top and bottom bounds of [char] relative to the text baseline, before bold and shadow are applied.
     * Returns an empty range if the character is missing.
     */
    fun getCharYRange(font: Key, char: Char): ClosedRange<Float> =
        getCharYRange(font, char.code)
    
    /**
     * Calculates the width of [string] when rendered with [font].
     */
    fun calculateStringWidth(font: Key, string: String): Float {
        val table = getTable(font) ?: return 0f
        val metrics = CharMetrics()
        var width = 0f
        string.codePoints().forEach { codePoint ->
            if (table.get(codePoint, metrics))
                width += metrics.width
        }
        return width
    }
    
    /**
     * Calculates the width of a component.
     */
    fun calculateComponentWidth(component: Component, lang: String = "en_us"): Float {
        return calculateComponentSize(component, lang, false).width
    }
    
    /**
     * Calculates the [ComponentSize] of the given [component] under [lang].
     *
     * [hasShadowByDefault] controls whether text without an explicit shadow setting is measured with a shadow.
     * An explicit shadow setting on the component takes precedence.
     */
    fun calculateComponentSize(component: Component, lang: String, hasShadowByDefault: Boolean): ComponentSize {
        return componentCache.get(Triple(component, lang, hasShadowByDefault)) {
            calculateComponentSize(component.elements(lang), hasShadowByDefault)
        }
    }
    
    /**
     * Calculates the [ComponentSize] from a sequence of [StyledElements][StyledElement].
     *
     * [hasShadowByDefault] controls whether text without an explicit shadow setting is measured with a shadow.
     * An explicit shadow setting on the element takes precedence.
     */
    fun calculateComponentSize(elements: Sequence<StyledElement>, hasShadowByDefault: Boolean): ComponentSize {
        var width = 0f
        
        var xRangeMin = Float.MAX_VALUE
        var xRangeMax = -Float.MAX_VALUE
        var yRangeMin = Float.MAX_VALUE
        var yRangeMax = -Float.MAX_VALUE
        val metrics = CharMetrics()
        
        for (element in elements) {
            if (element is StyledElement.CodePoint) {
                val fontKey = element.style.font() ?: Key.key("minecraft", "default")
                val shadowColor = element.style.shadowColor()
                val hasShadow = shadowColor?.alpha()?.let { it > 0 } ?: hasShadowByDefault
                val isBold = element.style.hasDecoration(TextDecoration.BOLD)
                val hasMetrics = getTable(fontKey)?.get(element.codePoint, metrics) == true
                val styleOffset = if (hasMetrics && metrics.halfOffset) 0.5f else 1f
                
                // x
                if (hasMetrics && metrics.xMin <= metrics.xMax) {
                    var charMinX = width + metrics.xMin
                    var charMaxX = width + metrics.xMax
                    
                    if (element.style.hasDecoration(TextDecoration.ITALIC) && metrics.yMin <= metrics.yMax) {
                        val shearTop = 1f - 0.25f * (metrics.yMin + FONT_BASELINE)
                        val shearBottom = 1f - 0.25f * (metrics.yMax + FONT_BASELINE)
                        charMinX += minOf(shearTop, shearBottom)
                        charMaxX += maxOf(shearTop, shearBottom)
                    }
                    
                    if (isBold) {
                        charMinX -= BOLD_EXTRA_THICKNESS
                        charMaxX += styleOffset + BOLD_EXTRA_THICKNESS
                    }
                    if (hasShadow)
                        charMaxX += styleOffset
                    
                    if (charMinX < xRangeMin) xRangeMin = charMinX
                    if (charMaxX > xRangeMax) xRangeMax = charMaxX
                }
                
                var charWidth = if (hasMetrics) metrics.width else 0f
                if (isBold) charWidth += styleOffset
                width += charWidth
                
                // y
                if (hasMetrics && metrics.yMin <= metrics.yMax) {
                    val boldThickness = if (isBold) BOLD_EXTRA_THICKNESS else 0f
                    val charMinY = metrics.yMin - boldThickness
                    val charMaxY = metrics.yMax + boldThickness + if (hasShadow) styleOffset else 0f
                    
                    if (charMinY < yRangeMin) yRangeMin = charMinY
                    if (charMaxY > yRangeMax) yRangeMax = charMaxY
                }
            } else if (element is StyledElement.Object) {
                val shadowColor = element.style.shadowColor()
                val hasShadow = shadowColor?.alpha()?.let { it > 0 } ?: hasShadowByDefault
                if (width < xRangeMin) xRangeMin = width
                val maxX = width + 8f + if (hasShadow) 1f else 0f
                if (maxX > xRangeMax) xRangeMax = maxX
                width += 8
                if (-8f < yRangeMin) yRangeMin = -8f
                val maxY = -1f + if (hasShadow) 1f else 0f
                if (maxY > yRangeMax) yRangeMax = maxY
            }
        }
        
        val xRange = if (xRangeMin <= xRangeMax) xRangeMin..xRangeMax else EMPTY_COMPONENT_RANGE
        val yRange = if (yRangeMin <= yRangeMax) yRangeMin..yRangeMax else EMPTY_COMPONENT_RANGE
        return ComponentSize(width, xRange, yRange)
    }
    
    @InitFun
    private fun init() {
        if (LOAD_CHAR_SIZES_ON_STARTUP) {
            val service = Executors.newCachedThreadPool()
            
            CHAR_SIZES_DIR.walk().filter(Path::isRegularFile).forEach {
                val fontKey = getFontKey(it)
                if (!loadedTables.containsKey(fontKey)) {
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
    
    /**
     * Invalidates all cached component measurements.
     */
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
    
    /**
     * Stores [table] for [font] and makes it available to runtime calculations.
     */
    internal fun storeTable(font: Key, table: CharSizeTable) {
        loadedTables[font] = table
        table.write(getFile(font))
    }
    
    /**
     * Gets the table for [font], loading it from disk on first access.
     */
    internal fun getTable(font: Key): CharSizeTable? {
        return loadedTables[font] ?: loadTable(font)
    }
    
    /**
     * Removes the loaded and stored table for [font].
     */
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

internal class CharMetrics {
    
    /**
     * Whether bold and shadow are moved by 0.5 pixels instead of 1 pixel.
     *
     * See [GlyphInfo](https://mcsrc.dev/2/26.2/com/mojang/blaze3d/font/GlyphInfo#L10-16) and
     * [UnihexProvider](https://mcsrc.dev/2/26.2/net/minecraft/client/gui/font/providers/UnihexProvider#L296-312).
     */
    var halfOffset = false
    
    var width = 0f
    var xMin = Float.MAX_VALUE
    var xMax = -Float.MAX_VALUE
    var yMin = Float.MAX_VALUE
    var yMax = -Float.MAX_VALUE
    
}

/**
 * An immutable lookup of character sizes for a font.
 */
internal sealed class CharSizeTable {
    
    /**
     * Copies the size of [char] into [result] and returns whether the character exists.
     */
    abstract fun get(char: Int, result: CharMetrics): Boolean
    
    /**
     * Gets the cursor advance of [char].
     */
    fun getWidth(char: Int): Float {
        val metrics = CharMetrics()
        return if (get(char, metrics)) metrics.width else 0f
    }
    
    /**
     * Gets the unstyled horizontal visual bounds of [char].
     */
    fun getXRange(char: Int): ClosedRange<Float> {
        val metrics = CharMetrics()
        return if (get(char, metrics)) metrics.xMin..metrics.xMax else NO_VISUAL_RANGE
    }
    
    /**
     * Gets the unstyled, baseline-relative vertical visual bounds of [char].
     */
    fun getYRange(char: Int): ClosedRange<Float> {
        val metrics = CharMetrics()
        return if (get(char, metrics)) metrics.yMin..metrics.yMax else NO_VISUAL_RANGE
    }
    
    /**
     * Gets the bold offset of [char].
     */
    fun getBoldOffset(char: Int): Float {
        val metrics = CharMetrics()
        return if (get(char, metrics) && metrics.halfOffset) 0.5f else 1f
    }
    
    /**
     * Gets the shadow offset of [char].
     */
    fun getShadowOffset(char: Int): Float {
        val metrics = CharMetrics()
        return if (get(char, metrics) && metrics.halfOffset) 0.5f else 1f
    }
    
    /**
     * Checks whether this table contains metrics for [char].
     */
    abstract operator fun contains(char: Int): Boolean
    
    /**
     * Writes this table to [file] using gzip compression.
     */
    fun write(file: Path) {
        file.parent.createDirectories()
        file.outputStream().buffered().let(::GZIPOutputStream).let(::DataOutputStream).use(::write)
    }
    
    internal abstract fun write(output: DataOutput)
    
    companion object {
        
        /**
         * Creates a table containing the given character sizes.
         */
        fun direct(sizes: Int2ObjectMap<FloatArray>): CharSizeTable =
            DirectCharSizeTable.from(sizes)
        
        /**
         * Creates a table that delegates to [font].
         */
        fun reference(font: Key): CharSizeTable =
            ReferenceCharSizeTable(font)
        
        /**
         * Creates a table that checks [tables] in order and uses the first matching character.
         */
        fun composite(tables: List<CharSizeTable>): CharSizeTable =
            when (tables.size) {
                0 -> DirectCharSizeTable.empty()
                1 -> tables.first()
                else -> CompositeCharSizeTable(tables)
            }
        
        /**
         * Creates a table that references [font] and shifts its vertical bounds by [verticalOffset].
         */
        fun moved(font: Key, verticalOffset: Int): CharSizeTable =
            MovedCharSizeTable(font, verticalOffset)
        
        /**
         * Loads a compressed character-size table from [file].
         */
        fun load(file: Path): CharSizeTable {
            DataInputStream(GZIPInputStream(BufferedInputStream(Files.newInputStream(file)))).use { input ->
                return read(input)
            }
        }
        
        private fun read(input: DataInput): CharSizeTable {
            return when (val type = input.readUnsignedByte()) {
                DIRECT_TABLE_TYPE -> DirectCharSizeTable.read(input)
                REFERENCE_TABLE_TYPE -> ReferenceCharSizeTable(Key.key(input.readUTF()))
                COMPOSITE_TABLE_TYPE -> {
                    val size = input.readInt()
                    require(size >= 0) { "Negative composite character-size table size: $size" }
                    CompositeCharSizeTable(List(size) { read(input) })
                }
                
                MOVED_TABLE_TYPE -> MovedCharSizeTable(Key.key(input.readUTF()), input.readInt())
                else -> throw IllegalArgumentException("Unknown character-size table type: $type")
            }
        }
        
    }
    
}

private class DirectCharSizeTable(
    private val indices: Int2IntOpenHashMap,
    private val metrics: FloatArray,
    private val halfOffsets: BitSet
) : CharSizeTable() {
    
    init {
        indices.defaultReturnValue(-1)
    }
    
    override fun get(char: Int, result: CharMetrics): Boolean {
        val index = indices[char]
        if (index == -1)
            return false
        
        val offset = index * METRICS_PER_CHAR
        result.width = metrics[offset]
        result.xMin = metrics[offset + 1]
        result.xMax = metrics[offset + 2]
        result.yMin = metrics[offset + 3]
        result.yMax = metrics[offset + 4]
        result.halfOffset = halfOffsets[index]
        return true
    }
    
    override fun contains(char: Int): Boolean =
        indices.containsKey(char)
    
    override fun write(output: DataOutput) {
        output.writeByte(DIRECT_TABLE_TYPE)
        output.writeInt(indices.size)
        
        val chars = indices.keys.toIntArray()
        chars.sort()
        for (char in chars) {
            val index = indices[char]
            val offset = index * METRICS_PER_CHAR
            output.writeInt(char)
            repeat(METRICS_PER_CHAR) { output.writeFloat(metrics[offset + it]) }
            output.writeBoolean(halfOffsets[index])
        }
    }
    
    companion object {
        
        private const val METRICS_PER_CHAR = 5
        
        fun empty(): DirectCharSizeTable =
            DirectCharSizeTable(Int2IntOpenHashMap(), FloatArray(0), BitSet())
        
        fun from(sizes: Int2ObjectMap<FloatArray>): DirectCharSizeTable {
            val indices = Int2IntOpenHashMap(sizes.size)
            indices.defaultReturnValue(-1)
            val metrics = FloatArray(sizes.size * METRICS_PER_CHAR)
            val halfOffsets = BitSet(sizes.size)
            
            sizes.int2ObjectEntrySet().forEachIndexed { index, entry ->
                val values = entry.value
                require(values.size == 7)
                
                val boldOffset = values[5]
                val shadowOffset = values[6]
                require(boldOffset == shadowOffset) {
                    "Bold and shadow offsets differ for code point ${entry.intKey}: $boldOffset != $shadowOffset"
                }
                require(boldOffset == 1f || boldOffset == 0.5f) {
                    "Unsupported bold and shadow offset for code point ${entry.intKey}: $boldOffset"
                }
                
                indices[entry.intKey] = index
                val metricOffset = index * METRICS_PER_CHAR
                values.copyInto(metrics, metricOffset, 0, METRICS_PER_CHAR)
                if (boldOffset == 0.5f)
                    halfOffsets[index] = true
            }
            
            return DirectCharSizeTable(indices, metrics, halfOffsets)
        }
        
        fun read(input: DataInput): DirectCharSizeTable {
            val size = input.readInt()
            require(size >= 0) { "Negative character-size table size: $size" }
            
            val indices = Int2IntOpenHashMap(size)
            indices.defaultReturnValue(-1)
            val metrics = FloatArray(size * METRICS_PER_CHAR)
            val halfOffsets = BitSet(size)
            
            repeat(size) { index ->
                indices[input.readInt()] = index
                val offset = index * METRICS_PER_CHAR
                repeat(METRICS_PER_CHAR) { metrics[offset + it] = input.readFloat() }
                if (input.readBoolean())
                    halfOffsets[index] = true
            }
            
            return DirectCharSizeTable(indices, metrics, halfOffsets)
        }
        
    }
    
}

private class ReferenceCharSizeTable(
    private val font: Key
) : CharSizeTable() {
    
    private val referencedTable: CharSizeTable by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CharSizes.getTable(font)
            ?: throw IllegalStateException("Referenced font ${font.asString()} has no character-size table")
    }
    
    override fun get(char: Int, result: CharMetrics): Boolean =
        referencedTable.get(char, result)
    
    override fun contains(char: Int): Boolean =
        char in referencedTable
    
    override fun write(output: DataOutput) {
        output.writeByte(REFERENCE_TABLE_TYPE)
        output.writeUTF(font.asString())
    }
    
}

private class CompositeCharSizeTable(
    private val tables: List<CharSizeTable>
) : CharSizeTable() {
    
    override fun get(char: Int, result: CharMetrics): Boolean =
        tables.any { it.get(char, result) }
    
    override fun contains(char: Int): Boolean =
        tables.any { char in it }
    
    override fun write(output: DataOutput) {
        output.writeByte(COMPOSITE_TABLE_TYPE)
        output.writeInt(tables.size)
        tables.forEach { it.write(output) }
    }
    
}

private class MovedCharSizeTable(
    private val font: Key,
    private val verticalOffset: Int
) : CharSizeTable() {
    
    private val source: CharSizeTable by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CharSizes.getTable(font)
            ?: throw IllegalStateException("Moved font source ${font.asString()} has no character-size table")
    }
    
    override fun get(char: Int, result: CharMetrics): Boolean {
        if (!source.get(char, result))
            return false
        
        result.yMin += verticalOffset
        result.yMax += verticalOffset
        return true
    }
    
    override fun contains(char: Int): Boolean =
        char in source
    
    override fun write(output: DataOutput) {
        output.writeByte(MOVED_TABLE_TYPE)
        output.writeUTF(font.asString())
        output.writeInt(verticalOffset)
    }
    
}