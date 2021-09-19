package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.lang.reflect.Type
import java.util.*

object ChunkSerialization : JsonSerializer<Chunk>, JsonDeserializer<Chunk> {
    
    override fun serialize(src: Chunk, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.add("worldUUID", GSON.toJsonTree(src.world.uid))
        jsonObject.addProperty("x", src.x)
        jsonObject.addProperty("z", src.z)
        
        return jsonObject
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Chunk {
        json as JsonObject
        
        val world: World = if (json.has("worldUUID"))
            Bukkit.getWorld(GSON.fromJson<UUID>(json.get("worldUUID"))!!)!!
        else Bukkit.getWorld(json.get("world").asString)!!
        
        val x = json.get("x").asInt
        val z = json.get("z").asInt
        return world.getChunkAt(x, z)
    }
    
}