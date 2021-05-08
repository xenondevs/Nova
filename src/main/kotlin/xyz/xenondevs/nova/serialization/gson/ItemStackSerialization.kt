package xyz.xenondevs.nova.serialization.gson

import com.google.gson.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Type
import java.util.*

object ItemStackSerializer : JsonSerializer<ItemStack> {
    
    override fun serialize(src: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val config = YamlConfiguration()
        config.set("item", src)
        val data = Base64.getEncoder().encodeToString(config.saveToString().toByteArray())
        return JsonPrimitive(data)
    }
    
}

object ItemStackDeserializer : JsonDeserializer<ItemStack> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        val data = json.asString
        val configString = String(Base64.getDecoder().decode(data))
        val config = YamlConfiguration.loadConfiguration(configString.reader())
        return config.getItemStack("item")!!
    }
    
}