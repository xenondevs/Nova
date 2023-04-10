package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.gson.getAllInts
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.DisplayEntityBlockModelData
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.data.serialization.json.getDeserializedOrNull
import java.lang.reflect.Type
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

// TODO: find a better solution
internal object BlockModelDataSerialization : JsonSerializer<BlockModelData>, JsonDeserializer<BlockModelData> {
    
    override fun serialize(src: BlockModelData, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val result = JsonObject()
        when (src) {
            is DisplayEntityBlockModelData -> {
                result.addProperty("id", src.id.toString())
                result.addProperty("hitboxType", src.hitboxType.name)
                result.add("dataArray", GSON.toJsonTree(src.dataArray))
            }
            
            is BlockStateBlockModelData -> {
                result.addProperty("id", src.id.toString())
                
                val data = JsonArray().also { result.add("data", it) }
                src.data.forEach { (face, blockStateConfigs) ->
                    blockStateConfigs.forEach { blockStateConfig ->
                        val obj = JsonObject().also { data.add(it) }
                        obj.addProperty("face", face.name)
                        obj.addProperty("type", blockStateConfig::class.jvmName)
                        obj.addProperty("id", blockStateConfig.id)
                    }
                }
            }
        }
        
        return result
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BlockModelData {
        json as JsonObject
        
        if (json.has("data")) {
            val id = json.getStringOrNull("id")!!
            val data = HashMap<BlockFace, ArrayList<BlockStateConfig>>()
            
            json.getAsJsonArray("data").forEach { obj ->
                obj as JsonObject
                
                val face: BlockFace = obj.getDeserializedOrNull("face")!!
                val type = Class.forName(obj.getStringOrNull("type")).kotlin.companionObjectInstance as BlockStateConfigType<BlockStateConfig>
                val blockStateId = obj.getIntOrNull("id")!!
                
                data.getOrPut(face, ::ArrayList) += type.of(blockStateId)
            }
            
            return BlockStateBlockModelData(ResourceLocation.of(id, ':'), data)
        } else {
            val id = json.getStringOrNull("id")!!
            val hitboxType = Material.valueOf(json.getStringOrNull("hitboxType")!!)
            val dataArray = json.getAsJsonArray("dataArray").getAllInts().toIntArray()
            
            return DisplayEntityBlockModelData(ResourceLocation.of(id, ':'), hitboxType, dataArray)
        }
    }
    
}