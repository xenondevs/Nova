package xyz.xenondevs.nova.resources.builder.task

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getOrPut
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

/**
 * Merges atlas files from asset packs and writes them to the resource pack.
 */
class AtlasTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val runAfter = setOf(ExtractTask::class)
    
    override suspend fun run() {
        val sources = HashMap<String, JsonArray>()
        
        builder.assetPacks.forEach { pack ->
            pack.atlasesDir?.walk()?.forEach { atlas ->
                val atlasName = atlas.nameWithoutExtension
                val atlasSources = sources.getOrPut(atlasName, ::JsonArray)
                atlasSources.addAll((atlas.parseJson() as JsonObject).getAsJsonArray("sources"))
            }
        }
        
        sources.forEach {
            val file = builder.resolve("assets/minecraft/atlases/${it.key}.json")
            file.parent.createDirectories()
            val atlasesObj = file.takeIf(Path::exists)?.parseJson() as? JsonObject ?: JsonObject()
            val sourcesJson = atlasesObj.getOrPut("sources", ::JsonArray)
            sourcesJson.addAll(it.value)
            atlasesObj.writeToFile(file)
        }
    }
    
}