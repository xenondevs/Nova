package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.bukkit.NamespacedKey
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.nova.serialization.json.GSON
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.loot.LootItem
import xyz.xenondevs.nova.world.loot.LootTable
import java.lang.reflect.Type

internal object LootTableSerialization : JsonDeserializer<LootTable> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): LootTable {
        val obj = json.asJsonObject
        val items = GSON.fromJson<ArrayList<LootItem>>(obj.get("items"))!!
        val whitelist = if (obj.has("whitelist")) GSON.fromJson<ArrayList<NamespacedKey>>(obj.get("whitelist"))!! else ArrayList()
        val blacklist = if (obj.has("blacklist")) GSON.fromJson<ArrayList<NamespacedKey>>(obj.get("blacklist"))!! else ArrayList()
        return LootTable(items, whitelist, blacklist)
    }
    
}

internal object LootItemSerialization : JsonDeserializer<LootItem> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): LootItem {
        val obj = json.asJsonObject
        val itemId = obj.get("item").asString
        if (!ItemUtils.isIdRegistered(itemId)) {
            throw IllegalArgumentException("Item with id $itemId is not registered")
        }
        val item = ItemUtils.getItemStack(itemId)
        val chance = obj.get("chance").asDouble
        val amount = GSON.fromJson<IntRange>(obj.get("amount"))!!
        
        return LootItem(item, chance, amount)
    }
    
    
}