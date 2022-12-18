package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.readImage
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.math.roundToInt

private const val START_CHAR = '\uF000'

internal abstract class FontContent<C : FontChar, D : FontContent.FontData<C>>(
    private val updateResources: ((Map<String, C>) -> Unit)? = null,
    private val generateMovedVariations: Boolean = false
) : PackContent {
    
    private var lastChar = START_CHAR
    private var fontId = 0
    
    private val lookup = HashMap<String, D>()
    
    fun addFontEntry(id: String, path: ResourcePath) {
        lookup[id] = createFontData(fontId, lastChar, path)
        
        if (lastChar == Char.MAX_VALUE) {
            lastChar = START_CHAR
            fontId++
        } else {
            lastChar++
        }
    }
    
    fun getFile(path: ResourcePath): Path {
        var file = ResourcePackBuilder.ASSETS_DIR.resolve("${path.namespace}/textures/${path.path}")
        if (!file.exists() && path.namespace == "minecraft") {
            file = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/textures/${path.path}")
        }
        if (!file.exists())
            throw FileNotFoundException("Missing texture file: $file")
        return file
    }
    
    fun getWidth(fontHeight: Int, path: ResourcePath): Int {
        return getWidth(fontHeight, getFile(path).readImage())
    }
    
    fun getWidth(fontHeight: Int, image: BufferedImage): Int {
        var maxX = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (x > maxX) {
                    val rgba = image.getRGB(x, y)
                    if (rgba ushr 24 != 0)
                        maxX = x
                }
            }
        }
        
        return ((maxX + 1) / image.width.toDouble() * fontHeight).roundToInt()
    }
    
    protected abstract fun createFontData(id: Int, char: Char, path: ResourcePath): D
    
    override fun write() {
        updateResources?.invoke(lookup.mapValuesTo(HashMap()) { it.value.toFontInfo() })
        if (lookup.isEmpty()) return
        
        lookup.entries
            .groupBy { it.value.font }
            .forEach { (font, entries) ->
                val dataList = entries.map { it.value }
                if (generateMovedVariations) {
                    val dir = ResourcePackBuilder.FONT_DIR.resolve(font.substringAfter(':'))
                    for (i in -20..0) {
                        val file = dir.resolve("$i.json")
                        writeFont(file, dataList, i)
                    }
                } else {
                    val file = ResourcePackBuilder.FONT_DIR.resolve("${font.substringAfter(':')}.json")
                    writeFont(file, dataList, 0)
                }
            }
    }
    
    private fun writeFont(file: Path, dataList: List<D>, ascent: Int) {
        val fontObj = JsonObject()
        val providers = JsonArray().also { fontObj.add("providers", it) }
        
        dataList.forEach { data ->
            val char = data.char
            val path = data.path
            
            val provider = JsonObject().apply(providers::add)
            provider.addProperty("type", "bitmap")
            provider.addProperty("file", path.toString())
            provider.addProperty("height", data.height)
            provider.addProperty("ascent", data.ascent + ascent)
            provider.add("chars", JsonArray().apply { add(char) })
        }
        
        file.parent.createDirectories()
        file.writeText(GSON.toJson(fontObj))
    }
    
    abstract class FontData<I : FontChar>(val font: String, val char: Char, val path: ResourcePath, val width: Int) {
        abstract val height: Int
        abstract val ascent: Int
        abstract fun toFontInfo(): I
    }
    
}

open class FontChar internal constructor(val font: String, val char: Char, val width: Int) {
    val component by lazy { TextComponent(char.toString()).also { it.font = font; it.color = ChatColor.WHITE } }
}