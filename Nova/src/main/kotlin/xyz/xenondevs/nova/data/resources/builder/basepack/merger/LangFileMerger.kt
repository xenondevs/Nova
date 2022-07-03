package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.set
import java.io.File

internal class LangFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/lang") {
    
    override fun merge(source: File, destination: File) {
        if (!destination.exists()) {
            source.copyTo(destination)
            return
        }
        
        val sourceObj = JsonParser.parseReader(source.reader()) as? JsonObject ?: return
        val destObj = JsonParser.parseReader(destination.reader()) as? JsonObject ?: return
        
        sourceObj.entrySet().forEach { (key, value) -> if (!destObj.has(key)) destObj[key] = value }
        
        destination.writeText(GSON.toJson(destObj))
    }
    
}