package xyz.xenondevs.nova.serialization.gson

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Chunk
import java.lang.reflect.Type

object ChunkSerializer : JsonSerializer<Chunk> {
    
    override fun serialize(src: Chunk, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("world", src.world.name)
        jsonObject.addProperty("x", src.x)
        jsonObject.addProperty("z", src.z)
        
        return jsonObject
    }
    
}

object ChunkDeserializer : JsonDeserializer<Chunk> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Chunk {
        json as JsonObject
        val world = Bukkit.getWorld(json.get("world").asString)!!
        val x = json.get("x").asInt
        val z = json.get("z").asInt
        return world.getChunkAt(x, z)
    }
    
}