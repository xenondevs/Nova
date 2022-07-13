package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.util.data.*
import java.lang.reflect.Type
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

// TODO: find a better solution
internal object BlockModelDataSerialization : JsonSerializer<BlockModelData>, JsonDeserializer<BlockModelData> {
    
    override fun serialize(src: BlockModelData, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val result = JsonObject()
        when (src) {
            is ArmorStandBlockModelData -> {
                result.addProperty("id", src.id)
                result.addProperty("hitboxType", src.hitboxType.name)
                result.addProperty("material", src.material.name)
                result.add("dataArray", GSON.toJsonTree(src.dataArray))
            }
            
            is BlockStateBlockModelData -> {
                result.addProperty("id", src.id)
                
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
            val id = json.getString("id")!!
            val data = HashMap<BlockFace, ArrayList<BlockStateConfig>>()
            
            json.getAsJsonArray("data").forEach { obj ->
                obj as JsonObject
                
                val face: BlockFace = obj.getDeserialized("face")!!
                val type = Class.forName(obj.getString("type")).kotlin.companionObjectInstance as BlockStateConfigType<BlockStateConfig>
                val blockStateId = obj.getInt("id")!!
                
                data.getOrPut(face, ::ArrayList) += type.of(blockStateId)
            }
            
            return BlockStateBlockModelData(id, data)
        } else {
            val id = json.getString("id")!!
            val hitboxType = Material.valueOf(json.getString("hitboxType")!!)
            val material = Material.valueOf(json.getString("material")!!)
            val dataArray = json.getAsJsonArray("dataArray").getAllInts().toIntArray()
            
            return ArmorStandBlockModelData(id, hitboxType, material, dataArray)
        }
    }
    
}