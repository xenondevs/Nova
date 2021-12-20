package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.*
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.data.*
import xyz.xenondevs.nova.world.loot.LootInfo
import java.lang.reflect.Type

object LootInfoDeserializer : JsonDeserializer<LootInfo> {
    
    override fun deserialize(obj: JsonElement, type: Type, ctx: JsonDeserializationContext): LootInfo {
        if (obj !is JsonObject)
            throw JsonParseException("Expected JsonObject")
        val itemName = obj.getString("item")!!.removePrefix("nova:").uppercase()
        val item = NovaMaterialRegistry.get(itemName)
        val frequency = obj.getDouble("frequency", 75.0)
        val (min, max) = getMinMax(obj)
        val (whitelist, blacklist) = getAllowLists(obj)
        
        return LootInfo(frequency, min, max, item, whitelist, blacklist)
    }
    
    private fun getMinMax(obj: JsonObject): Pair<Int, Int> {
        val min: Int
        val max: Int
        if (obj.hasNumber("amount")) {
            min = obj.getInt("amount")!!
            max = min
        } else {
            min = obj.getInt("min", 1)
                .coerceAtLeast(1)
            max = obj.getInt("max", 4)
                .coerceAtLeast(min)
        }
        return min to max
    }
    
    private fun getAllowLists(obj: JsonObject): Pair<List<NamespacedKey>, List<NamespacedKey>> {
        val blacklist: List<NamespacedKey>
        val whitelist: List<NamespacedKey>
        if (obj.hasArray("blacklist")) {
            blacklist = getKeyList(obj.getAsJsonArray("blacklist"))
            whitelist = emptyList()
        } else if (obj.hasArray("whitelist")) {
            whitelist = getKeyList(obj.getAsJsonArray("whitelist"))
            blacklist = emptyList()
        } else {
            blacklist = emptyList()
            whitelist = emptyList()
        }
        return whitelist to blacklist
    }
    
    
    private fun getKeyList(array: JsonArray) =
        array.filter { it.isString() }.map { NamespacedKey.fromString(it.asString)!! }
    
}