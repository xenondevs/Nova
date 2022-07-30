package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.parseJson
import java.io.File

internal class FontFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/lang") {
    
    override fun merge(source: File, destination: File) {
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