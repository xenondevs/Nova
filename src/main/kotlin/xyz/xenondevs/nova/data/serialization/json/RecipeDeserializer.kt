package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.*
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.GearPressNovaRecipe
import xyz.xenondevs.nova.data.recipe.PlatePressNovaRecipe
import xyz.xenondevs.nova.data.recipe.PulverizerNovaRecipe
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.MaterialUtils
import xyz.xenondevs.nova.util.data.*
import java.io.File

@Suppress("LiftReturnOrAssignment", "CascadeIf")
private fun getItemBuilder(name: String): ItemBuilder {
    try {
        if (name.startsWith("nova:")) {
            return NovaMaterialRegistry.get(name.substringAfter(':').uppercase()).createItemBuilder()
        } else if (name.startsWith("minecraft:")) {
            return ItemBuilder(Material.valueOf(name.substringAfter(':').uppercase()))
        } else throw IllegalArgumentException("Invalid item name: $name")
    } catch (ex: Exception) {
        throw IllegalArgumentException("Unknown item $name", ex)
    }
}

private fun getRecipeKey(file: File): NamespacedKey =
    NamespacedKey(NOVA, "nova.${file.parentFile.name}.${file.nameWithoutExtension}")

interface RecipeDeserializer<T> {
    
    fun deserialize(json: JsonObject, file: File): T
    
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
            ingredients.entrySet().forEach { (char, value) ->
                val nameList = when {
                    value is JsonArray -> value.asJsonArray.map(JsonElement::getAsString)
                    value.isString() -> listOf(value.asString)
                    else -> throw IllegalArgumentException()
                }
                ingredientMap[char[0]] = MaterialUtils.getRecipeChoice(nameList)
            }
        } else if (ingredients is JsonArray) {
            // legacy support
            ingredients.forEach {
                it as JsonObject
                val char = it.getString("char")!![0]
                val item = it.getString("item")!!
                ingredientMap[char] = MaterialUtils.getRecipeChoice(listOf(item))
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
                val choice = MaterialUtils.getRecipeChoice(listOf(key))
                ingredientsMap[choice] = value.asInt
            }
        } else if (ingredients is JsonArray) {
            ingredients.forEach {
                it as JsonObject
                
                val items = it.get("item") ?: it.get("items")
                val choice = MaterialUtils.getRecipeChoice(
                    when {
                        items is JsonArray -> items.getAllStrings()
                        items.isString() -> listOf(items.asString)
                        else -> throw IllegalArgumentException("Item name(s) expected")
                    }
                )
                
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

abstract class ConversionRecipeDeserializer<T> : RecipeDeserializer<T> {
    
    override fun deserialize(json: JsonObject, file: File): T {
        val input = json.get("input")
        val nameList = when {
            input is JsonArray -> input.getAllStrings()
            input.isString() -> listOf(input.asString)
            else -> throw IllegalArgumentException()
        }
        val inputChoice = MaterialUtils.getRecipeChoice(nameList)
        
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

object PulverizerRecipeDeserializer : ConversionRecipeDeserializer<PulverizerNovaRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        PulverizerNovaRecipe(key, input, result, time)
}

object PlatePressRecipeDeserializer : ConversionRecipeDeserializer<PlatePressNovaRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        PlatePressNovaRecipe(key, input, result, time)
}

object GearPressRecipeDeserializer : ConversionRecipeDeserializer<GearPressNovaRecipe>() {
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int) =
        GearPressNovaRecipe(key, input, result, time)
}
