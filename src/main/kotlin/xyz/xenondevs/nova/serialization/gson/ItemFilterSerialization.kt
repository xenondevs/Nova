package xyz.xenondevs.nova.serialization.gson

import com.google.gson.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.network.item.ItemFilter
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson
import java.lang.reflect.Type

object ItemFilterSerializer : JsonSerializer<ItemFilter> {
    
    override fun serialize(src: ItemFilter, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("whitelist", src.whitelist)
        jsonObject.add("items", GSON.toJsonTree(src.items))
        return jsonObject
    }
    
}

object ItemFilterDeserializer : JsonDeserializer<ItemFilter> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemFilter {
        json as JsonObject
        val whitelist = json.get("whitelist").asBoolean
        val items: Array<ItemStack?> = GSON.fromJson(json.get("items"))!!
        return ItemFilter(whitelist, items)
    }
    
}
