package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.SolidBlockModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.getAllInts
import xyz.xenondevs.nova.util.data.getDeserialized
import xyz.xenondevs.nova.util.data.getString
import java.lang.reflect.Type
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
            
            is SolidBlockModelData<*> -> {
                result.addProperty("type", src.type::class.jvmName)
                result.addProperty("id", src.id)
                result.add("data", GSON.toJsonTree(src.data.mapValues { it.value.map(BlockStateConfig::id) }))
            }
        }
        
        return result
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BlockModelData {
        json as JsonObject
        
        if (json.has("type")) {
            val type = Class.forName(json.getString("type")).kotlin.objectInstance!! as BlockStateConfigType<BlockStateConfig>
            val id = json.getString("id")!!
            val data = json.getDeserialized<Map<BlockFace, List<Int>>>("data")!!.mapValues { it.value.map(type::of) }
            
            return SolidBlockModelData(type, id, data)
        } else {
            val id = json.getString("id")!!
            val hitboxType = Material.valueOf(json.getString("hitboxType")!!)
            val material = Material.valueOf(json.getString("material")!!)
            val dataArray = json.getAsJsonArray("dataArray").getAllInts().toIntArray()
            
            return ArmorStandBlockModelData(id, hitboxType, material, dataArray)
        }
    }
    
}