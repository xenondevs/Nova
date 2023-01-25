package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getOrPut
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.data.walk
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

internal class AtlasContent : PackContent {
    
    override val stage = ResourcePackBuilder.BuildingStage.PRE_WORLD
    
    private val sources = HashMap<String, JsonArray>()
    
    override fun includePack(pack: AssetPack) {
        val atlasesDir = pack.atlasesDir ?: return
        pack.zip.walk(atlasesDir, false).forEach {
            val atlasName = it.fileName.substringAfterLast('/').substringBefore('.')
            val atlasSources = sources.getOrPut(atlasName, ::JsonArray)
            atlasSources.addAll((pack.zip.getInputStream(it).parseJson() as JsonObject).getAsJsonArray("sources"))
        }
    }
    
    override fun write() {
        sources.forEach {
            val file = ResourcePackBuilder.ASSETS_DIR.resolve("minecraft/atlases/${it.key}.json")
            file.parent.createDirectories()
            val atlasesObj = file.takeIf(Path::exists)?.parseJson() as? JsonObject ?: JsonObject()
            val sourcesJson = atlasesObj.getOrPut("sources", ::JsonArray)
            sourcesJson.addAll(it.value)
            atlasesObj.writeToFile(file)
        }
    }
    
}