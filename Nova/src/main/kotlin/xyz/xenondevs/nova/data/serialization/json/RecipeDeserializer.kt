package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.*
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.ItemUtils.getItemBuilder
import java.io.File

interface RecipeDeserializer<T> {
    
    fun deserialize(json: JsonObject, file: File): T
    
    fun parseRecipeChoice(element: JsonElement): RecipeChoice {
        val nameList = when {
            element is JsonArray -> element.getAllStrings()
            element.isString() -> listOf(element.asString)
            else -> throw IllegalArgumentException()
        }.map { ids ->
            // Id fallbacks
            ids.replace(" ", "")
                .split(';')
                .firstOrNull(ItemUtils::isIdRegistered)
                ?: throw IllegalArgumentException("Invalid item id(s): $ids")
        }
        
        return ItemUtils.getRecipeChoice(nameList)
    }
    
    fun getRecipeKey(file: File): NamespacedKey =
        NamespacedKey(NOVA, "${file.parentFile.name}.${file.nameWithoutExtension}")
    
}

object ShapedRecipeDeserializer : RecipeDeserializer<ShapedRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapedRecipe {
        val resultKey = json.getString("result")!!
        
        val result = getItemBuilder(resultKey)
            .setAmount(json.getInt("amount", default = 1))
            .get()
        
        val shape = json.getAsJsonArray("shape").toStringList(::ArrayList)
        val ingredientMap = HashMap<Char, RecipeChoice>()
        
        val ingredients = json.get("ingredients")
        if (ingredients is JsonObject) {
            ingredients.entrySet().forEach { (char, value) -> ingredientMap[char[0]] = parseRecipeChoice(value) }
        } else if (ingredients is JsonArray) {
            // legacy support
            ingredients.forEach {
                it as JsonObject
                val char = it.getString("char")!![0]
                val item = it.getString("item")!!
                ingredientMap[char] = ItemUtils.getRecipeChoice(listOf(item))
            }
        }
        
        val recipe = ShapedRecipe(getRecipeKey(file), result)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        return recipe
    }
    
}

object ShapelessRecipeDeserializer : RecipeDeserializer<ShapelessRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapelessRecipe {
        val resultKey = json.getString("result")!!
        
        val result = getItemBuilder(resultKey)
            .setAmount(json.getInt("amount", default = 1))
            .get()
        
        val ingredientsMap = HashMap<RecipeChoice, Int>()
        val ingredients = json.get("ingredients")
        if (ingredients is JsonObject) {
            ingredients.entrySet().forEach { (key, value) ->
                val choice = ItemUtils.getRecipeChoice(listOf(key))
                ingredientsMap[choice] = value.asInt
            }
        } else if (ingredients is JsonArray) {
            ingredients.forEach {
                it as JsonObject
                
                val items = it.get("item") ?: it.get("items")
                val choice = parseRecipeChoice(items)
                ingredientsMap[choice] = it.getInt("amount")!!
            }
        }
        
        val recipe = ShapelessRecipe(getRecipeKey(file), result)
        ingredientsMap.forEach { (material, count) ->
            var amountLeft = count
            while (amountLeft-- > 0) {
                recipe.addIngredient(material)
            }
        }
        
        return recipe
    }
    
}

object StonecutterRecipeDeserializer : RecipeDeserializer<StonecuttingRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): StonecuttingRecipe {
        val input = parseRecipeChoice(json.get("input"))
        val result = getItemBuilder(json.get("result").asString)
        result.amount = json.getInt("amount", 1)
        return StonecuttingRecipe(getRecipeKey(file), result.get(), input)
    }
    
}

object SmithingRecipeDeserializer : RecipeDeserializer<SmithingRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): SmithingRecipe {
        val base = parseRecipeChoice(json.get("base"))
        val addition = parseRecipeChoice(json.get("addition"))
        val result = getItemBuilder(json.get("result").asString)
        result.amount = json.getInt("amount", 1)
        return SmithingRecipe(getRecipeKey(file), result.get(), base, addition)
    }
    
}

abstract class ConversionRecipeDeserializer<T> : RecipeDeserializer<T> {
    
    override fun deserialize(json: JsonObject, file: File): T {
        val inputChoice = parseRecipeChoice(json.get("input"))
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        
        val time = json.getInt("time") ?: json.getInt("cookingTime")!! // legacy support
        
        return createRecipe(json, getRecipeKey(file), inputChoice, result.get(), time)
    }
    
    abstract fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int): T
    
}

object FurnaceRecipeDeserializer : ConversionRecipeDeserializer<FurnaceRecipe>() {
    
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int): FurnaceRecipe {
        val experience = json.getFloat("experience")!!
        return FurnaceRecipe(key, result, input, experience, time)
    }
    
}
