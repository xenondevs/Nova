package xyz.xenondevs.nova.resources.builder.task.basepack.merger

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.resources.builder.task.basepack.BasePacks
import xyz.xenondevs.nova.serialization.json.GSON
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.writeText

internal class FontFileMerger(basePacks: BasePacks) : FileInDirectoryMerger(basePacks, "assets/minecraft/font") {
    
    override fun merge(source: Path, destination: Path) {
        if (!destination.exists()) {
            source.copyTo(destination)
            return
        }
        
        val sourceObj = source.parseJson() as? JsonObject ?: return
        val destObj = destination.parseJson() as? JsonObject ?: return
        val sourceProviders = sourceObj.get("providers") as? JsonArray ?: return
        val destProviders = destObj.get("providers") as? JsonArray ?: return
        
        destProviders.addAll(sourceProviders)
        
        destination.writeText(GSON.toJson(destObj))
    }
    
}