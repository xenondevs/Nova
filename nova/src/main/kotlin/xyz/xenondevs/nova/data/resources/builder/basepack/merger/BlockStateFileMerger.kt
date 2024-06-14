package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.addAll
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.BrownMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MushroomStemBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.NoteBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

private val IGNORABLE_MODELS: Set<String> = hashSetOf(
    "minecraft:block/original/brown_mushroom_block_true" // ItemsAdder
)

private val MERGEABLE_STATE_CONFIGS = listOf(
    NoteBackingStateConfig,
    RedMushroomBackingStateConfig,
    BrownMushroomBackingStateConfig,
    MushroomStemBackingStateConfig
)

internal class BlockStateFileMerger(basePacks: BasePacks) : FileInDirectoryMerger(basePacks, "assets/minecraft/blockstates") {
    
    override fun merge(source: Path, destination: Path) {
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
    
    private fun getVariants(file: Path): JsonObject {
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
    
    private fun processVariants(configType: BackingStateConfigType<*>, obj: JsonObject) {
        try {
            val occupied = basePacks.occupiedSolidIds.getOrPut(configType, ::HashSet)
            obj.entrySet().removeIf { (variant, obj) ->
                obj as JsonObject
                val model = obj.getStringOrNull("model")
                if (model in IGNORABLE_MODELS)
                    return@removeIf true
                
                occupied += configType.of(variant).id
                
                return@removeIf false
            }
    
            configType.handleMerged(occupied)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to process variants for $configType in $obj", e)
        }
    }
    
}