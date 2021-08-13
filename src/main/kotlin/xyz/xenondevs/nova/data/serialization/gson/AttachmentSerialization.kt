package xyz.xenondevs.nova.data.serialization.gson

import com.google.gson.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.player.attachment.Attachment
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import xyz.xenondevs.nova.util.data.getBoolean
import xyz.xenondevs.nova.util.data.getString
import java.lang.reflect.Type
import java.util.*

object AttachmentSerializer : JsonSerializer<Attachment> {
    
    override fun serialize(src: Attachment, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("key", src.key)
        jsonObject.add("uuid", GSON.toJsonTree(src.playerUUID))
        jsonObject.add("itemStack", GSON.toJsonTree(src.itemStack))
        jsonObject.addProperty("hideOnDown", src.hideOnDown)
        
        return jsonObject
    }
    
}

object AttachmentDeserializer : JsonDeserializer<Attachment> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Attachment {
        json as JsonObject
        val key: String = json.getString("key")!!
        val uuid: UUID = GSON.fromJson(json.get("uuid"))!!
        val itemStack: ItemStack = GSON.fromJson(json.get("itemStack"))!!
        val hideOnDown: Boolean = json.getBoolean("hideOnDown")
        
        return Attachment(key, uuid, itemStack, hideOnDown)
    }
    
}