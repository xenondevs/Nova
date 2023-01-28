package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists

internal class AtlasFileMerger(basePacks: BasePacks) : FileInDirectoryMerger(basePacks, "assets/minecraft/atlases") {
    
    override fun merge(source: Path, destination: Path) {
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