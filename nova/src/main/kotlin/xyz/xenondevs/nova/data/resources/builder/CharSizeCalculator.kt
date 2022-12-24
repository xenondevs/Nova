package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.CharSizeTable
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.encodeWithBase64
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.math.roundToInt

private val FONT_NAME_REGEX = Regex("""^([a-z0-9._-]+)/font/([a-z0-9/._-]+)$""")

internal class CharSizeCalculator {
    
    private val fontHashes: HashMap<String, String> = PermanentStorage.retrieve("fontHashes", ::HashMap)
    
    private val bitmaps = HashMap<ResourcePath, BufferedImage>()
    private val glyphSizes = HashMap<ResourcePath, ByteArray>()
    
    fun calculateCharSizes() {
        val fontDirs = ArrayList<Path>()
        fontDirs += ResourcePackBuilder.ASSETS_DIR.listDirectoryEntries()
            .mapNotNull { it.resolve("font/").takeIf(Path::exists) }
        // order is required: vanilla fonts need to be loaded after custom fonts in order to keep overridden values
        fontDirs.add(ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/font/"))
        
        fontDirs.forEach { dir ->
            dir.walk()
                .filter { !it.isDirectory() && it.extension == "json" }
                .forEach inner@{ file ->
                    val font = getFontName(dir.parent.parent, file)
                    
                    val fileHash = HashUtils.getFileHash(file, "MD5").encodeWithBase64()
                    if (fontHashes[font] == fileHash) 
                        return@inner
                    
                    CharSizes.deleteTable(font)
                    fontHashes[font] = fileHash
                    
                    val table = CharSizes.getTable(font) ?: CharSizeTable()
                    calculateCharSizes(file, table)
                    CharSizes.storeTable(font, table)
                }
        }
        
        PermanentStorage.store("fontHashes", fontHashes)
    }
    
    private fun getFontName(base: Path, file: Path): String {
        val relPath = file.relativeTo(base)
            .invariantSeparatorsPathString
            .substringBeforeLast('.') // example: minecraft/font/default
        
        val result = FONT_NAME_REGEX.matchEntire(relPath)
            ?: throw IllegalArgumentException("File $file is not a font file")
        
        return "${result.groupValues[1]}:${result.groupValues[2]}"
    }
    
    private fun calculateCharSizes(file: Path, table: CharSizeTable) {
        try {
            val obj = file.parseJson() as JsonObject
            val providers = obj.getAsJsonArray("providers")
            providers.forEach { provider ->
                provider as JsonObject
                when (val type = provider.getString("type")) {
                    "space" -> readSpaceProvider(provider, table)
                    "bitmap" -> readBitmapProvider(provider, table)
                    "legacy_unicode" -> readUnicodeProvider(provider, table)
                    "ttf" -> LOGGER.warning("Skipping size calculation for ttf font provider: $provider")
                    else -> LOGGER.warning("Unknown font provider type: $type")
                }
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to calculate char sizes for $file", e)
        }
    }
    
    private fun readSpaceProvider(obj: JsonObject, table: CharSizeTable) {
        val advances = obj.getAsJsonObject("advances")
        advances.entrySet().forEach { (str, size) ->
            val sizeInt = size.asInt
            readChars(str).forEach { table.setWidth(it, sizeInt) }
        }
    }
    
    private fun readBitmapProvider(obj: JsonObject, table: CharSizeTable) {
        val bitmap = getBitmap(ResourcePath.of(obj.getString("file")!!, "minecraft"))
        val height = obj.getInt("height", 8)
        val ascent = obj.getInt("ascent", 0)
        val chars = obj.getAsJsonArray("chars").getAllStrings().map(::readChars)
        
        val charsPerLine = chars[0].size
        val lines = chars.size
        
        require(bitmap.width % charsPerLine == 0) { "Bitmap width is not divisible by amount of chars per line" }
        require(bitmap.height % lines == 0) { "Bitmap height is not divisible by amount of char lines" }
        
        val subimageWidth = bitmap.width / charsPerLine
        val subimageHeight = bitmap.height / lines
        
        chars.forEachIndexed { lineIdx, charsInLine ->
            charsInLine.forEachIndexed { charIdx, char ->
                if (char !in table) {
                    val subimage = bitmap.getSubimage(charIdx * subimageWidth, lineIdx * subimageHeight, subimageWidth, subimageHeight)
                    
                    table.setSizes(
                        char,
                        intArrayOf(
                            calculateBitmapCharWidth(height, subimage) + 1, // +1 to include space between characters
                            height,
                            ascent
                        )
                    )
                }
            }
        }
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
    
    private fun readUnicodeProvider(obj: JsonObject, table: CharSizeTable) {
        val sizes = getGlyphSizes(ResourcePath.of(obj.getString("sizes")!!))
        val buffer = Unpooled.wrappedBuffer(sizes)
        
        // legacy_unicode can only have codepoints from U+0000 to U+FFFF
        for (c in 0..0xFFFF) {
            val charInfo = buffer.readUnsignedByte().toInt()
            
            if (c in table)
                continue
            
            val start = charInfo shr 4 and 0xF
            val end = charInfo and 0xF
            val width = end - start + 1
            
            table.setWidth(c, width + 1) // +1 to include space between characters
        }
    }
    
    private fun readChars(str: String): IntArray {
        val chars = ArrayList<Int>()
        
        var highSurrogate: Char? = null
        fun addChar(code: Int) {
            val char = code.toChar()
            
            if (highSurrogate != null) {
                if (char.isLowSurrogate()) {
                    chars += Character.toCodePoint(highSurrogate!!, char)
                }
                
                highSurrogate = null
            } else if (char.isHighSurrogate()) {
                highSurrogate = char
            } else {
                chars += code
            }
        }
        
        var i = 0
        while (i < str.length) {
            val c = str[i]
            
            if (str.lastIndex >= i + 5 && c == '\\' && str[i + 1] == 'u') {
                addChar(Integer.valueOf(str.substring(i + 2, i + 6), 16))
                i += 6
                continue
            }
            
            addChar(c.code)
            i++
        }
        
        return chars.toIntArray()
    }
    
    private fun getBitmap(path: ResourcePath): BufferedImage {
        return bitmaps.getOrPut(path) {
            val file = getAssetFile("${path.namespace}/textures/${path.path}")
            return@getOrPut file.readImage()
        }
    }
    
    private fun getGlyphSizes(path: ResourcePath): ByteArray {
        return glyphSizes.getOrPut(path) {
            val file = getAssetFile("${path.namespace}/${path.path}")
            return@getOrPut file.readBytes()
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