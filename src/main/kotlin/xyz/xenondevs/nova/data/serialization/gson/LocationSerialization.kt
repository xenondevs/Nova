package xyz.xenondevs.nova.data.serialization.gson

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Type

object LocationSerializer : JsonSerializer<Location> {
    
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
}

object LocationDeserializer : JsonDeserializer<Location> {
    
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