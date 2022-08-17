package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.GSON
import java.io.File

private const val START_CHAR = '\u0001'

internal abstract class FontContent<C : FontChar, D : FontContent.FontData<C>>(
    private val updateResources: ((Map<String, C>) -> Unit)? = null,
    private val generateMovedVariations: Boolean = false
) : PackContent {
    
    private var lastChar = START_CHAR
    private var fontId = 0
    
    private val lookup = HashMap<String, D>()
    
    fun addFontEntry(id: String, path: String) {
        lookup[id] = createFontData(fontId, lastChar, path.replace('\\', '/'))
        
        if (lastChar == Char.MAX_VALUE) {
            lastChar = START_CHAR
            fontId++
        } else {
            lastChar++
        }
    }
    
    protected abstract fun createFontData(id: Int, char: Char, path: String): D
    
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
            provider.addProperty("file", path)
            provider.addProperty("height", data.height)
            provider.addProperty("ascent", data.ascent + ascent)
            provider.add("chars", JsonArray().apply { add(char) })
        }
        
        file.parentFile.mkdirs()
        file.writeText(GSON.toJson(fontObj))
    }
    
    abstract class FontData<I : FontChar>(val font: String, val char: Char, val path: String) {
        abstract val height: Int
        abstract val ascent: Int
        abstract fun toFontInfo(): I
    }
    
}

open class FontChar internal constructor(val font: String, val char: Char)