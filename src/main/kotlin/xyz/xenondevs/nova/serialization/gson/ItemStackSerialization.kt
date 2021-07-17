package xyz.xenondevs.nova.serialization.gson

import com.google.gson.*
import de.studiocode.inventoryaccess.version.InventoryAccess
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

object ItemStackSerializer : JsonSerializer<ItemStack> {
    
    override fun serialize(src: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(Base64.getEncoder().encodeToString(InventoryAccess.getItemUtils().serializeItemStack(src, true)))
    }
    
}

object ItemStackDeserializer : JsonDeserializer<ItemStack> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        return InventoryAccess.getItemUtils().deserializeItemStack(Base64.getDecoder().decode(json.asString), true)
    }
    
}