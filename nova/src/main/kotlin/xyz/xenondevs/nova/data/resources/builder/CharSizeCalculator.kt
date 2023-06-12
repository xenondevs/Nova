package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.gson.getAllStrings
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.CharSizeTable
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.content.PackAnalyzer
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.encodeWithBase64
import xyz.xenondevs.nova.util.data.font.Font
import xyz.xenondevs.nova.util.data.font.UnihexProvider
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.walk
import kotlin.math.roundToInt

private const val FONT_HASHES_STORAGE_KEY = "fontHashes0.14"

class CharSizeCalculator private constructor() {
    
    companion object : PackAnalyzer {
        
        override fun analyze(builder: ResourcePackBuilder) {
            LOGGER.info("Calculating char sizes")
            CharSizeCalculator().calculateCharSizes()
        }
        
        internal fun invalidateFontHashesStorage() {
            PermanentStorage.remove(FONT_HASHES_STORAGE_KEY)
        }
        
    }
    
    /**
     * Contains the hashes of font files from the last session.
     */
    private val fontHashes: HashMap<String, String> = PermanentStorage.retrieve(FONT_HASHES_STORAGE_KEY, ::HashMap)
    
    /**
     * Stores which files affect a given font.
     * Required for vanilla overrides.
     * Does not include reference providers.
     */
    private val fontRelations = HashMap<ResourcePath, MutableList<Path>>()
    
    /**
     * A cache for bitmaps in this session.
     */
    private val bitmaps = HashMap<ResourcePath, BufferedImage>()
    
    /**
     * Calculates the char sizes for all fonts that need to be recalculated.
     */
    internal fun calculateCharSizes() {
        val customFontDirs = ResourcePackBuilder.ASSETS_DIR.listDirectoryEntries()
            .mapNotNull { it.resolve("font/").takeIf(Path::exists) }
        val vanillaFontDir = listOf(ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/font/"))
        
        // load custom fonts
        val fonts = collectFonts(customFontDirs).associateByTo(HashMap(), Font::id)
        
        // merge vanilla fonts with custom fonts
        // this is required because resource packs do not replace fonts by overriding them, they just add providers
        collectFonts(vanillaFontDir).forEach { vanillaFont ->
            val id = vanillaFont.id
            
            val fontOverride = fonts[id]
            if (fontOverride != null) {
                fontOverride.merge(vanillaFont)
            } else {
                fonts[id] = vanillaFont
            }
        }
        
        // load references (maps reference provider ids to the font instance they reference)
        for (font in fonts.values) font.loadReferences(fonts.values)
        
        // stores the fonts that needed to be recalculated
        val recalculated = HashSet<Font>()
        
        CollectionUtils.sortDependencies(fonts.values, Font::referenceProviders)
            .forEach { font ->
                val id = font.id
                
                // hashes of files affecting this font's json
                val fileHashes: Map<String, String> = fontRelations[font.id]!!
                    .associate { it.absolutePathString() to HashUtils.getFileHash(it, "MD5").encodeWithBase64() }
                
                // if any of the affecting files changed or any of the reference providers changed, recalculate char sizes for this font
                if (fileHashes.any { (path, hash) -> fontHashes[path] != hash } || font.referenceProviders.any { it in recalculated }) {
                    // recalculate char sizes
                    val table = CharSizeTable()
                    calculateCharSizes(font, table)
                    CharSizes.storeTable(id, table)
                    
                    // mark font as recalculated
                    recalculated += font
                    
                    // store new file hashes
                    fontHashes.putAll(fileHashes)
                }
            }
        
        PermanentStorage.store(FONT_HASHES_STORAGE_KEY, fontHashes)
    }
    
    /**
     * Collects all fonts from the given [directories][dirs].
     */
    private fun collectFonts(dirs: Iterable<Path>): List<Font> {
        val fonts = ArrayList<Font>()
        for (dir in dirs) {
            val baseDir = dir.parent.parent
            dir.walk()
                .filter { !it.isDirectory() && it.extension == "json" }
                .forEach {
                    val font = Font.fromFile(baseDir, it)
                    fonts += font
                    fontRelations.getOrPut(font.id, ::ArrayList).add(it)
                }
        }
        return fonts
    }
    
    private fun calculateCharSizes(font: Font, table: CharSizeTable) {
        try {
            for (provider in font.providers.reversed()) { // reverse providers so that top providers override bottom providers
                provider as JsonObject
                when (val type = provider.getStringOrNull("type")) {
                    "space" -> readSpaceProvider(provider, table)
                    "bitmap" -> readBitmapProvider(provider, table)
                    "unihex" -> readUnihexProvider(provider, table)
                    "reference" -> readReferenceProvider(provider, table)
                    "ttf" -> LOGGER.warning("Skipping size calculation for ttf font provider: $provider")
                    else -> LOGGER.warning("Unknown font provider type: $type")
                }
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to calculate char sizes for $font", e)
        }
    }
    
    private fun readSpaceProvider(obj: JsonObject, table: CharSizeTable) {
        val advances = obj.getAsJsonObject("advances")
        advances.entrySet().forEach { (str, size) ->
            val sizeInt = size.asInt
            str.codePoints().forEach { table.setWidth(it, sizeInt) }
        }
    }
    
    private fun readBitmapProvider(obj: JsonObject, table: CharSizeTable) {
        val bitmap = getBitmap(ResourcePath.of(obj.getStringOrNull("file")!!, "minecraft"))
        val height = obj.getIntOrNull("height") ?: 8
        val ascent = obj.getIntOrNull("ascent") ?: 0
        val codePoints = obj.getAsJsonArray("chars").getAllStrings().map { it.codePoints().toArray() }
        
        val codePointsPerLine = codePoints[0].size
        val lines = codePoints.size
        
        require(bitmap.width % codePointsPerLine == 0) { "Bitmap width is not divisible by amount of chars per line" }
        require(bitmap.height % lines == 0) { "Bitmap height is not divisible by amount of char lines" }
        
        val subimageWidth = bitmap.width / codePointsPerLine
        val subimageHeight = bitmap.height / lines
        
        codePoints.forEachIndexed { lineIdx, codePointsInLine ->
            codePointsInLine.forEachIndexed { codePointIdx, codePoint ->
                val subimage = bitmap.getSubimage(codePointIdx * subimageWidth, lineIdx * subimageHeight, subimageWidth, subimageHeight)
                
                val yRange = calculateBitmapCharYRange(height, ascent, subimage)
                table.setSizes(
                    codePoint,
                    intArrayOf(
                        calculateBitmapCharWidth(height, subimage) + 1, // +1 to include space between characters
                        height,
                        ascent,
                        yRange.first,
                        yRange.last
                    )
                )
            }
        }
    }
    
    private fun readUnihexProvider(obj: JsonObject, table: CharSizeTable) {
        UnihexProvider.of(obj).glyphSequence().forEach { glyph ->
            val (left, right) = glyph.findHorizontalBounds() ?: return@forEach
            val (top, bottom) = glyph.findVerticalBounds() ?: return@forEach
            
            // unihex glyphs render at gui scale 2, but all char sizes at stored at gui scale 1, so we divide by 2
            // fixme: if glyph sizes are not divisible by 2, this will cause issues. char sizes should be stored at gui scale 2
            table.setSizes(glyph.codePoint, intArrayOf(
                (right - left + 1) / 2 + 1, // +1 because bounds are inclusive, +1 to include space between characters
                glyph.img.height / 2,
                7, // == floor(15.0 / 2.0), same ascent as ascii bitmap characters
                top / 2,
                bottom / 2
            ))
        }
    }
    
    private fun readReferenceProvider(obj: JsonObject, table: CharSizeTable) {
        val id = ResourcePath.of(obj.getString("id"))
        val referencedTable = CharSizes.getTable(id)
            ?: throw IllegalStateException("No char size table for referenced font $id")
        table.merge(referencedTable)
    }
    
    private fun calculateBitmapCharWidth(fontHeight: Int, image: BufferedImage): Int {
        var maxX = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (x > maxX) {
                    val rgba = image.getRGB(x, y)
                    if (rgba ushr 24 != 0) {
                        maxX = x
                    }
                }
            }
        }
        
        val rescale = fontHeight / image.height.toDouble()
        return ((maxX + 1) * rescale).roundToInt()
    }
    
    private fun calculateBitmapCharYRange(fontHeight: Int, ascent: Int, image: BufferedImage): IntRange {
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgba = image.getRGB(x, y)
                if (rgba ushr 24 != 0) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        
        val rescale = fontHeight / image.height.toDouble()
        minY = ((minY - ascent) * rescale).roundToInt()
        maxY = ((maxY - ascent) * rescale).roundToInt()
        return minY..maxY
    }
    
    private fun getBitmap(path: ResourcePath): BufferedImage {
        return bitmaps.getOrPut(path) {
            val file = getAssetFile("${path.namespace}/textures/${path.path}")
            return@getOrPut file.readImage()
        }
    }
    
    private fun getAssetFile(path: String): Path {
        var file = ResourcePackBuilder.ASSETS_DIR.resolve(path)
        if (!file.exists()) {
            file = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/$path")
            if (!file.exists())
                throw FileNotFoundException(file.pathString)
        }
        
        return file
    }
    
}