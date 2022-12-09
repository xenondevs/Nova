package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.writeToFile
import java.io.File

internal class AtlasFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/atlases") {
    
    override fun merge(source: File, destination: File) {
        if (destination.exists()) {
            val destJson = destination.parseJson() as JsonObject
            val sourceAtlasSources = (source.parseJson() as JsonObject).getAsJsonArray("sources")
            val destAtlasSources = destJson.getAsJsonArray("sources")
            destAtlasSources.addAll(sourceAtlasSources)
            destJson.writeToFile(destination)
        } else {
            source.copyTo(destination)
        }
    }
    
}