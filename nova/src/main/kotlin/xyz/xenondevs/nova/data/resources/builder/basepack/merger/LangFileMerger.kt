package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonObject
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.set
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