package xyz.xenondevs.nova.data.resources.builder.content.font

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.data.readImageDimensions
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private const val START_CHAR = '\uF000'

abstract class FontContent(
    private val fontTemplate: String,
    private val updateResources: ((Map<String, FontChar>) -> Unit)? = null,
) : PackContent {
    
    private var currentChar = START_CHAR
    private var fontId = 0
    
    private val chars = HashMap<String, BitmapFontChar>()
    
    protected abstract val movedFontContent: MovedFontContent?
    
    fun addFontEntry(id: String, image: ResourcePath, height: Int?, ascent: Int) {
        chars[id] = BitmapFontChar(
            fontTemplate.format(fontId),
            currentChar,
            image,
            height ?: image.findInAssets("textures").readImageDimensions().height,
            ascent
        )
        
        if (currentChar == Char.MAX_VALUE) {
            currentChar = START_CHAR
            fontId++
        } else {
            currentChar++
        }
    }
    
    override fun write() {
        updateResources?.invoke(chars.mapValuesTo(HashMap()) { it.value.fontChar })
        if (chars.isEmpty()) return
        
        chars.entries
            .groupBy { it.value.fontChar.font }
            .forEach { (font, entries) ->
                val dataList = entries.map { it.value }
                val file = ResourcePackBuilder.FONT_DIR.resolve("${font.substringAfter(':')}.json")
                writeFont(file, dataList)
                movedFontContent?.requestMovedFonts(ResourcePath.of(font), 1..19)
            }
    }
    
    private fun writeFont(file: Path, chars: List<BitmapFontChar>) {
        val fontObj = JsonObject()
        val providers = JsonArray().also { fontObj.add("providers", it) }
        
        chars.forEach { data ->
            val char = data.fontChar.char
            val path = data.imagePath
            
            val provider = JsonObject().apply(providers::add)
            provider.addProperty("type", "bitmap")
            provider.addProperty("file", path.toString())
            provider.addProperty("height", data.height)
            provider.addProperty("ascent", data.ascent)
            provider.add("chars", JsonArray().apply { add(char) })
        }
        
        file.parent.createDirectories()
        file.writeText(GSON.toJson(fontObj))
    }
    
}

private class BitmapFontChar(
    val font: String,
    val char: Char,
    val imagePath: ResourcePath,
    val height: Int,
    val ascent: Int
) {
    
    val fontChar: FontChar
        get() = FontChar(font, char)
    
}

class FontChar internal constructor(val font: String, val char: Char) {
    
    val width by lazy { CharSizes.getCharWidth(font, char) }
    val height by lazy { CharSizes.getCharHeight(font, char) }
    val ascent by lazy { CharSizes.getCharAscent(font, char) }
    
    val component: Component
        get() = Component.text()
            .content(char.toString())
            .font(font)
            .build()
    
}