package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.blockstate.BrownMushroomBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.MushroomStemBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.NoteBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.RedMushroomBlockStateConfig
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.addAll
import xyz.xenondevs.nova.util.data.parseJson
import java.io.File
import java.util.logging.Level

private val MERGEABLE_STATE_CONFIGS = listOf(
    NoteBlockStateConfig,
    RedMushroomBlockStateConfig,
    BrownMushroomBlockStateConfig,
    MushroomStemBlockStateConfig
)

internal class BlockStateFileMerger(basePacks: BasePacks) : FileMerger(basePacks, "assets/minecraft/blockstates") {
    
    override fun merge(source: File, destination: File) {
        val configType = MERGEABLE_STATE_CONFIGS.firstOrNull { it.fileName == source.nameWithoutExtension }
        if (configType != null) {
            val variants = getVariants(source)
            if (destination.exists())
                variants.addAll(getVariants(destination))
            
            processVariants(configType, variants)
            
            val blockStateObj = JsonObject()
            blockStateObj.add("variants", variants)
            destination.writeText(GSON.toJson(blockStateObj))
            return
        }
        
        source.copyTo(destination)
    }
    
    private fun getVariants(file: File): JsonObject {
        val sourceObj = file.parseJson() as JsonObject
        return sourceObj.get("variants") as? JsonObject
            ?: (sourceObj.get("multipart") as? JsonArray)?.let(::convertMultipartToVariants)
            ?: JsonObject()
    }
    
    private fun convertMultipartToVariants(array: JsonArray): JsonObject {
        val variants = JsonObject()
        
        try {
            array.forEach { obj ->
                obj as JsonObject
                val whenObj = obj.get("when") as JsonObject
                val apply = obj.get("apply")
                val model = (if (apply is JsonObject) apply.get("model") else apply.asJsonArray.first().asJsonObject.get("model")).asString
                
                val variantString = whenObj.entrySet().joinToString(",") { "${it.key}=${it.value.asString}" }
                variants.add(variantString, JsonObject().apply { addProperty("model", model) })
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to convert multipart to variants, some block states might be missing", e)
        }
        
        return variants
    }
    
    private fun processVariants(configType: BlockStateConfigType<*>, obj: JsonObject) {
        try {
            val occupied = basePacks.occupiedSolidIds.getOrPut(configType, ::HashSet)
            obj.entrySet().forEach { (variant, _) ->
                occupied += configType.of(variant).id
            }
    
            configType.handleMerged(occupied)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to process variants for $configType in $obj", e)
        }
    }
    
}