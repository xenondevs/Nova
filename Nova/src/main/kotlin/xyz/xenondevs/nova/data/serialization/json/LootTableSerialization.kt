package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.loot.LootItem
import xyz.xenondevs.nova.world.loot.LootTable
import java.lang.reflect.Type

object LootTableSerialization : JsonDeserializer<LootTable> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): LootTable {
        val obj = json.asJsonObject
        val items = GSON.fromJson<ArrayList<LootItem>>(obj.get("items"))!!
        val blacklisted = if (obj.has("blacklisted")) GSON.fromJson<ArrayList<NamespacedKey>>(obj.get("blacklisted"))!! else ArrayList()
        val whitelisted = if (obj.has("whitelisted")) GSON.fromJson<ArrayList<NamespacedKey>>(obj.get("whitelisted"))!! else ArrayList()
        return LootTable(items, blacklisted, whitelisted)
    }
    
    
}

object LootItemSerialization : JsonDeserializer<LootItem> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): LootItem {
        val obj = json.asJsonObject
        val itemId = obj.get("item").asString
        if (!ItemUtils.isIdRegistered(itemId)) {
            throw IllegalArgumentException("Item with id $itemId is not registered")
        }
        val item = ItemUtils.getItemBuilder(itemId)
        val chance = obj.get("chance").asDouble
        val amount = GSON.fromJson<IntRange>(obj.get("amount"))!!
        
        return LootItem(item, chance, amount)
    }
    
    
}