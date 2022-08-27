package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.GSON
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
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
    
    fun getFile(path: ResourcePath): File {
        var file = File(ResourcePackBuilder.ASSETS_DIR, "${path.namespace}/textures/${path.path}")
        if (!file.exists() && path.namespace == "minecraft") {
            file = File(ResourcePackBuilder.MCASSETS_DIR, "assets/minecraft/textures/${path.path}")
        }
        if (!file.exists())
            throw FileNotFoundException("Missing texture file: ${file.absolutePath}")
        return file
    }
    
    fun getWidth(fontHeight: Int, path: ResourcePath): Int {
        return getWidth(fontHeight, ImageIO.read(getFile(path)))
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
                    val dir = File(ResourcePackBuilder.FONT_DIR, font.substringAfter(':'))
                    for (i in -20..0) {
                        val file = File(dir, "$i.json")
                        writeFont(file, dataList, i)
                    }
                } else {
                    val file = File(ResourcePackBuilder.FONT_DIR, "${font.substringAfter(':')}.json")
                    writeFont(file, dataList, 0)
                }
            }
    }
    
    private fun writeFont(file: File, dataList: List<D>, ascent: Int) {
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
        
        file.parentFile.mkdirs()
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