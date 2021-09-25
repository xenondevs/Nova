package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.lang.reflect.Type

object ItemFilterSerialization : JsonSerializer<ItemFilter>, JsonDeserializer<ItemFilter> {
    
    override fun serialize(src: ItemFilter, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("whitelist", src.whitelist)
        jsonObject.add("items", GSON.toJsonTree(src.items))
        return jsonObject
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemFilter {
        json as JsonObject
        val whitelist = json.get("whitelist").asBoolean
        val items: Array<ItemStack?> = GSON.fromJson(json.get("items"))!!
        return ItemFilter(whitelist, true, items)
    }
    
}