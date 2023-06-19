package xyz.xenondevs.nova.data.resources.builder.font.provider.unihex

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.font.provider.FontProvider
import xyz.xenondevs.nova.data.serialization.json.addSerialized
import xyz.xenondevs.nova.data.serialization.json.getDeserialized
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.walk

/**
 * Specifies custom glyph sizes.
 *
 * @param from The first code point of the range
 * @param to The last code point of the range
 * @param left The left border in the glyph texture
 * @param right The right border in the glyph texture
 */
data class SizeOverride(val from: Int, val to: Int, val left: Int, val right: Int)

/**
 * Represents a `unihex` font provider.
 */
abstract class UnihexProvider internal constructor(
    val hexFile: ResourcePath
) : FontProvider() {
    
    protected abstract val sizeOverrides: ObjectList<SizeOverride>
    protected abstract val glyphs: UnihexGlyphs
    
    protected var glyphsChanged = false
    
    /**
     * Contains all glyph rasters sorted by width.
     *
     * Format: `<width, <codepoint, raster>>`
     */
    val glyphRasters by lazy(::createGlyphRasters)
    
    override val charSizes: Int2ObjectMap<FloatArray> by lazy(::calculateCharSizes)
    override val codePoints: IntSet
        get() {
            val set = IntOpenHashSet()
            set.addAll(glyphs.glyphs8.keys)
            set.addAll(glyphs.glyphs16.keys)
            set.addAll(glyphs.glyphs24.keys)
            set.addAll(glyphs.glyphs32.keys)
            return set
        }
    
    /**
     * Gets the glyph for the given code point.
     * @return The glyph as a pair in the format (width, glyph)
     */
    fun getGlyph(codePoint: Int): Pair<Int, IntArray>? {
        return glyphs.glyphs8.get(codePoint)?.let { 8 to it }
            ?: glyphs.glyphs16.get(codePoint)?.let { 16 to it }
            ?: glyphs.glyphs24.get(codePoint)?.let { 24 to it }
            ?: glyphs.glyphs32.get(codePoint)?.let { 32 to it }
    }
    
    /**
     * Adds the given [glyph] of [width] for the given [codePoint].
     */
    fun addGlyph(codePoint: Int, width: Int, glyph: IntArray) {
        when (width) {
            8 -> glyphs.glyphs8.put(codePoint, glyph)
            16 -> glyphs.glyphs16.put(codePoint, glyph)
            24 -> glyphs.glyphs24.put(codePoint, glyph)
            32 -> glyphs.glyphs32.put(codePoint, glyph)
            else -> throw IllegalArgumentException("Invalid glyph width: $width")
        }
        
        glyphsChanged = true
    }
    
    /**
     * Removes the glyph for the given [codePoint].
     */
    fun removeGlyph(codePoint: Int) {
        glyphs.glyphs8.remove(codePoint)
        glyphs.glyphs16.remove(codePoint)
        glyphs.glyphs24.remove(codePoint)
        glyphs.glyphs32.remove(codePoint)
        
        glyphsChanged = true
    }
    
    /**
     * Adds the given [sizeOverride] to the list of size overrides.
     */
    fun addSizeOverride(sizeOverride: SizeOverride) {
        sizeOverrides.add(sizeOverride)
    }
    
    /**
     * Removes the given [sizeOverride] from the list of size overrides.
     */
    fun removeSizeOverride(sizeOverride: SizeOverride) {
        sizeOverrides.remove(sizeOverride)
    }
    
    private fun createGlyphRasters(): Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArray>> {
        val rasters = Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArray>>()
        
        fun createForWidth(width: Int, glyphs: Int2ObjectMap<IntArray>) {
            for (entry in glyphs.int2ObjectEntrySet()) {
                val codePoint = entry.intKey
                val glyph = entry.value
                
                val (left, right) = calculateBounds(codePoint, width, glyph) ?: continue
                val imgWidth = right - left + 1
                
                // getOrPut boxes int
                val widthMap: Int2ObjectMap<IntArray>
                if (!rasters.containsKey(imgWidth)) {
                    widthMap = Int2ObjectOpenHashMap()
                    rasters.put(imgWidth, widthMap)
                } else widthMap = rasters.get(imgWidth)
                
                val raster = UnihexGlyphs.createArgbRaster(width, glyph, left, right, 0xFFFFFFFF.toInt(), 0x01000000)
                widthMap.put(codePoint, raster)
            }
        }
        
        createForWidth(8, glyphs.glyphs8)
        createForWidth(16, glyphs.glyphs16)
        createForWidth(24, glyphs.glyphs24)
        createForWidth(32, glyphs.glyphs32)
        
        return rasters
    }
    
    private fun calculateCharSizes(): Int2ObjectMap<FloatArray> {
        val sizes = Int2ObjectOpenHashMap<FloatArray>()
        
        fun calcForWidth(width: Int, glyphs: Int2ObjectMap<IntArray>) {
            for (entry in glyphs.int2ObjectEntrySet()) {
                val codePoint = entry.intKey
                val glyph = entry.value
                
                val (left, right) = calculateBounds(codePoint, width, glyph) ?: continue
                val (top, bottom) = UnihexGlyphs.findHorizontalBorders(glyph) ?: continue
                
                sizes.put(codePoint, floatArrayOf(
                    // +1 because bounds are inclusive
                    // /2 because they're rendered at gui-scale 2
                    // +1 for spacing between characters
                    // integer operations because the next character always starts at the next integer (e.g 4 -> 5, 4.5 -> 5)
                    ((right - left + 1) / 2 + 1).toFloat(), 
                    
                    // /2 because they're rendered at gui-scale 2
                    top / 2f, bottom / 2f
                ))
            }
        }
        
        calcForWidth(8, glyphs.glyphs8)
        calcForWidth(16, glyphs.glyphs16)
        calcForWidth(24, glyphs.glyphs24)
        calcForWidth(32, glyphs.glyphs32)
        
        return sizes
    }
    
    private fun calculateBounds(codePoint: Int, width: Int, glyph: IntArray): IntArray? {
        val sizeOverride = sizeOverrides.firstOrNull { codePoint in it.from..it.to }
        
        val left: Int
        val right: Int
        if (sizeOverride == null) {
            val borders = UnihexGlyphs.findVerticalBorders(width, glyph) ?: return null
            left = borders[0]
            right = borders[1]
        } else {
            left = sizeOverride.left
            right = sizeOverride.right
        }
        
        return intArrayOf(left, right)
    }
    
    override fun write(assetsDir: Path) {
        if (!glyphsChanged)
            return
        
        val zipFile = hexFile.getPath(assetsDir)
        zipFile.parent.createDirectories()
        zipFile.deleteIfExists()
        
        zipFile.useZip(true) { root ->
            val f = root.resolve("nova-generated.hex")
            glyphs.writeUnihexFile(f)
        }
    }
    
    override fun toJson() = JsonObject().apply {
        addProperty("type", "unihex")
        addProperty("hex_file", hexFile.toString())
        addSerialized("size_overrides", sizeOverrides)
    }
    
    private class Custom(hexFile: ResourcePath) : UnihexProvider(hexFile) {
        
        override val sizeOverrides = ObjectArrayList<SizeOverride>()
        override val glyphs = UnihexGlyphs()
        
        init {
            // always write glyphs
            glyphsChanged = true
        }
        
    }
    
    private class LazyLoaded(
        private val assetsDir: Path,
        hexFile: ResourcePath,
        sizeOverrides: List<SizeOverride>
    ) : UnihexProvider(hexFile) {
        
        override val sizeOverrides = ObjectArrayList(sizeOverrides)
        override val glyphs by lazy(::loadGlyphs)
        
        private fun loadGlyphs(): UnihexGlyphs {
            val glyphs = UnihexGlyphs()
            
            // hexFile: zip file that contains .hex files
            hexFile.getPath(assetsDir).useZip { zip ->
                zip.walk()
                    .filter { it.extension == "hex" }
                    .forEach { glyphs.merge(UnihexGlyphs.readUnihexFile(it)) }
            }
            
            return glyphs
        }
        
    }
    
    companion object {
        
        /**
         * Creates a new [UnihexProvider] without any glyphs.
         *
         * @param hexFile The hex zip file where glyphs will be written to.
         */
        fun create(hexFile: ResourcePath): UnihexProvider =
            Custom(hexFile)
        
        /**
         * Reads a [UnihexProvider] from disk.
         *
         * @param assetsDir The assets directory to use for reading the bitmap image.
         * @param provider The json object containing the provider data.
         */
        fun fromDisk(assetsDir: Path, provider: JsonObject): UnihexProvider {
            val hexFile = provider.getDeserialized<ResourcePath>("hex_file")
            val sizeOverrides = provider.getDeserialized<List<SizeOverride>>("size_overrides")
            return LazyLoaded(assetsDir, hexFile, sizeOverrides)
        }
        
    }
    
}