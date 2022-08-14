package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.removeNamespace
import java.io.File

private const val START_CHAR = '\u0000'
private const val WIDTH = 32
private const val ASCENT = -4

internal class WailaContent : PackContent {
    
    private val wailaLookup = HashMap<String, WailaIconData>()
    private var font = 0
    private var char = START_CHAR
    
    override fun addFromPack(pack: AssetPack) {
        val wailaDir = pack.texturesDir?.let { File(it, "waila/") }
        if (wailaDir == null || !wailaDir.exists())
            return
        
        wailaDir.walkTopDown().forEach { file ->
            if (file.isDirectory || !file.extension.equals("png", true))
                return@forEach
            
            val idNamespace = pack.namespace.takeUnless { it == "nova" } ?: "minecraft" // all textures form "nova" asset pack are for minecraft blocks
            val id = "$idNamespace:${file.nameWithoutExtension}"
            val path = "${pack.namespace}:waila/${file.name}"
            
            wailaLookup[id] = WailaIconData("nova:waila_textures_$font", char, path)
            
            if (char == '\uFFFF') {
                char = START_CHAR
                font++
            } else {
                char++
            }
        }
    }
    
    override fun write() {
        Resources.updateWailaDataLookup(wailaLookup)
        if (wailaLookup.isEmpty()) return
        
        wailaLookup.entries
            .groupBy { it.value.font }
            .forEach { (font, entries) ->
                val fontObj = JsonObject()
                val providers = JsonArray().also { fontObj.add("providers", it) }
                
                entries.forEach { (_, data) ->
                    val (_, char, path) = data
                    
                    val provider = JsonObject().apply(providers::add)
                    provider.addProperty("type", "bitmap")
                    provider.addProperty("file", path)
                    provider.addProperty("height", WIDTH)
                    provider.addProperty("ascent", ASCENT)
                    provider.add("chars", JsonArray().apply { add(char) })
                }
                
                val fontFile = File(ResourcePackBuilder.FONT_DIR, "${font.removeNamespace("nova")}.json")
                fontFile.parentFile.mkdirs()
                fontFile.writeText(GSON.toJson(fontObj))
            }
    }
    
}

data class WailaIconData(val font: String, val char: Char, val path: String)