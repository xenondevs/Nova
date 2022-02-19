package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.removeNamespace
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

internal class GUIContent(private val builder: ResourcePackBuilder) : PackContent {
    
    private val guiLookup = HashMap<String, GUIData>()
    private var char = '\uF000'
    
    override fun addFromPack(pack: AssetPack) {
        pack.guisIndex?.forEach { (id, path) ->
            try {
                val file = File(pack.texturesDir, path.removeNamespace(pack.namespace))
                val image = ImageIO.read(file)
                
                guiLookup[id] = GUIData(char, path, image.width, image.height)
                char++
            } catch (ex: IOException) {
                throw IOException("Failed to load gui texture $path")
            }
        }
    }
    
    override fun write() {
        Resources.updateGuiDataLookup(guiLookup)
        if (guiLookup.isEmpty()) return
        
        val guiObj = JsonObject()
        val providers = JsonArray().also { guiObj.add("providers", it) }
        
        guiLookup.forEach { (_, info) ->
            val (char, path, _, height) = info
            
            val provider = JsonObject().apply(providers::add)
            provider.addProperty("file", path)
            provider.addProperty("height", height)
            provider.addProperty("ascent", 13)
            provider.addProperty("type", "bitmap")
            provider.add("chars", JsonArray().apply { add(char) })
        }
        
        builder.guisFile.parentFile.mkdirs()
        builder.guisFile.writeText(GSON.toJson(guiObj))
    }
    
}

data class GUIData(val char: Char, val path: String, val width: Int, val height: Int)
