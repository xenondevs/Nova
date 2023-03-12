package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Type

internal object LocationSerialization : JsonSerializer<Location>, JsonDeserializer<Location> {
    
    override fun serialize(src: Location, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("x", src.x)
        obj.addProperty("y", src.y)
        obj.addProperty("z", src.z)
        obj.addProperty("yaw", src.yaw)
        obj.addProperty("pitch", src.pitch)
        obj.addProperty("world", src.world?.name ?: "")
        return obj
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Location {
        val obj = json.asJsonObject
        val x = obj.getAsJsonPrimitive("x").asDouble
        val y = obj.getAsJsonPrimitive("y").asDouble
        val z = obj.getAsJsonPrimitive("z").asDouble
        val yaw = obj.getAsJsonPrimitive("yaw").asFloat
        val pitch = obj.getAsJsonPrimitive("pitch").asFloat
        val worldName = obj.getAsJsonPrimitive("world").asString
        
        return Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch)
    }
    
}