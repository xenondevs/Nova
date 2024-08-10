package xyz.xenondevs.nova.resources.builder.basepack.merger

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.set
import xyz.xenondevs.nova.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.serialization.json.GSON
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.writeText

internal class LangFileMerger(basePacks: BasePacks) : FileInDirectoryMerger(basePacks, "assets/minecraft/lang") {
    
    override fun merge(source: Path, destination: Path) {
        if (!destination.exists()) {
            source.copyTo(destination)
            return
        }
        
        val sourceObj = source.parseJson() as? JsonObject ?: return
        val destObj = destination.parseJson() as? JsonObject ?: return
        
        sourceObj.entrySet().forEach { (key, value) -> if (!destObj.has(key)) destObj[key] = value }
        
        destination.writeText(GSON.toJson(destObj))
    }
    
}