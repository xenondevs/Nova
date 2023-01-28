package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import xyz.xenondevs.inventoryaccess.InventoryAccess
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

internal object ItemStackSerialization : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    
    override fun serialize(src: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(Base64.getEncoder().encodeToString(InventoryAccess.getItemUtils().serializeItemStack(src, true)))
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        return InventoryAccess.getItemUtils().deserializeItemStack(Base64.getDecoder().decode(json.asString), true)
    }
    
}