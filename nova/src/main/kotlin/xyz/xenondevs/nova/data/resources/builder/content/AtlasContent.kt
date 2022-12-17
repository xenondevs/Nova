package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.writeToFile
import java.io.File

internal class AtlasContent : PackContent {
    
    private val sources = HashMap<String, JsonArray>()
    
    override fun addFromPack(pack: AssetPack) {
        pack.atlasesDir?.listFiles()?.forEach { 
            val atlasName = it.nameWithoutExtension
            val atlasSources = sources.getOrPut(atlasName, ::JsonArray)
            atlasSources.addAll((it.parseJson() as JsonObject).getAsJsonArray("sources"))
        }
    }
    
    override fun write() {
        sources.forEach { 
            val file = File(ResourcePackBuilder.ASSETS_DIR, "minecraft/atlases/${it.key}.json")
            file.parentFile.mkdirs()
            val atlasesObj = file.takeIf(File::exists)?.parseJson() as? JsonObject ?: JsonObject()
            atlasesObj.add("sources", it.value)
            atlasesObj.writeToFile(file)
        }
    }

}