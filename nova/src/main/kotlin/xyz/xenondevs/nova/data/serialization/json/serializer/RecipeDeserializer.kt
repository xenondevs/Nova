package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import org.bukkit.inventory.recipe.CookingBookCategory
import org.bukkit.inventory.recipe.CraftingBookCategory
import xyz.xenondevs.commons.gson.getAllStrings
import xyz.xenondevs.commons.gson.getFloatOrNull
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.isString
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer.Companion.getRecipeKey
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer.Companion.parseRecipeChoice
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.ItemUtils.getItemBuilder
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

interface RecipeDeserializer<T> {
    
    fun deserialize(json: JsonObject, file: File): T
    
    companion object {
        
        fun parseRecipeChoice(element: JsonElement): RecipeChoice {
            val list = when {
                element is JsonArray -> element.getAllStrings()
                element.isString() -> listOf(element.asString)
                else -> throw IllegalArgumentException()
            }
            
            return parseRecipeChoice(list)
        }
        
        fun parseRecipeChoice(list: List<String>): RecipeChoice {
            val names = list.map { ids ->
                // Id fallbacks
                ids.replace(" ", "")
                    .split(';')
                    .firstOrNull {
                        if (it.startsWith('#')) {
                            val tagName = NamespacedKey.fromString(it.substringAfter('#'))
                                ?: throw IllegalArgumentException("Malformed tag: $it")
                            return@firstOrNull Bukkit.getTag(Tag.REGISTRY_ITEMS, tagName, Material::class.java) != null
                        } else ItemUtils.isIdRegistered(it.substringBefore('{'))
                    } ?: throw IllegalArgumentException("Invalid item id(s): $ids")
            }
            
            return ItemUtils.getRecipeChoice(names)
        }
        
        fun getRecipeKey(file: File): NamespacedKey =
            NamespacedKey(NOVA, "${file.parentFile.name}.${file.nameWithoutExtension}")
        
    }
    
}

internal object ShapedRecipeDeserializer : RecipeDeserializer<ShapedRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapedRecipe {
        val resultKey = json.getString("result")
        
        val result = getItemBuilder(resultKey)
            .setAmount(json.getIntOrNull("amount") ?: 1)
            .get()
        
        val shape = json.getAsJsonArray("shape").getAllStrings()
        val ingredientMap = HashMap<Char, RecipeChoice>()
        
        val ingredients = json.get("ingredients")
        if (ingredients is JsonObject) {
            ingredients.entrySet().forEach { (char, value) -> ingredientMap[char[0]] = parseRecipeChoice(value) }
        } else if (ingredients is JsonArray) {
            // legacy support
            ingredients.forEach {
                it as JsonObject
                val char = it.getStringOrNull("char")!![0]
                val item = it.getStringOrNull("item")!!
                ingredientMap[char] = ItemUtils.getRecipeChoice(listOf(item))
            }
        }
        
        val recipe = ShapedRecipe(getRecipeKey(file), result)
        recipe.shape(*shape.toTypedArray())
        ingredientMap.forEach { (key, material) -> recipe.setIngredient(key, material) }
        
        val category = json.getStringOrNull("category")
            ?.let { CraftingBookCategory.valueOf(it.uppercase()) }
            ?: CraftingBookCategory.MISC
        recipe.category = category
        
        return recipe
    }
    
}

internal object ShapelessRecipeDeserializer : RecipeDeserializer<ShapelessRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapelessRecipe {
        val resultKey = json.getStringOrNull("result")!!
        
        val result = getItemBuilder(resultKey)
            .setAmount(json.getIntOrNull("amount") ?: 1)
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
                ingredientsMap[choice] = it.getIntOrNull("amount")!!
            }
        }
        
        val recipe = ShapelessRecipe(getRecipeKey(file), result)
        ingredientsMap.forEach { (material, count) ->
            var amountLeft = count
            while (amountLeft-- > 0) {
                recipe.addIngredient(material)
            }
        }
        
        val category = json.getStringOrNull("category")
            ?.let { CraftingBookCategory.valueOf(it.uppercase()) }
            ?: CraftingBookCategory.MISC
        recipe.category = category
        
        return recipe
    }
    
}

internal object StonecutterRecipeDeserializer : RecipeDeserializer<StonecuttingRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): StonecuttingRecipe {
        val input = parseRecipeChoice(json.get("input"))
        val result = getItemBuilder(json.get("result").asString)
        result.amount = json.getIntOrNull("amount") ?: 1
        return StonecuttingRecipe(getRecipeKey(file), result.get(), input)
    }
    
}

internal object SmithingRecipeDeserializer : RecipeDeserializer<SmithingRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): SmithingRecipe {
        val base = parseRecipeChoice(json.get("base"))
        val addition = parseRecipeChoice(json.get("addition"))
        val result = getItemBuilder(json.get("result").asString)
        result.amount = json.getIntOrNull("amount") ?: 1
        return SmithingRecipe(getRecipeKey(file), result.get(), base, addition)
    }
    
}

abstract class ConversionRecipeDeserializer<T> : RecipeDeserializer<T> {
    
    override fun deserialize(json: JsonObject, file: File): T {
        val inputChoice = parseRecipeChoice(json.get("input"))
        
        val result = getItemBuilder(json.getStringOrNull("result")!!)
        result.amount = json.getIntOrNull("amount") ?: 1
        
        val time = json.getIntOrNull("time") ?: json.getIntOrNull("cookingTime")!! // legacy support
        
        return createRecipe(json, getRecipeKey(file), inputChoice, result.get(), time)
    }
    
    abstract fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int): T
    
}

@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
internal abstract class CookingRecipeDeserializer<T : CookingRecipe<T>>(
    val recipeConstructor: (key: NamespacedKey, result: ItemStack, input: RecipeChoice, experience: Float, time: Int) -> T
) : ConversionRecipeDeserializer<T>() {
    
    override fun createRecipe(json: JsonObject, key: NamespacedKey, input: RecipeChoice, result: ItemStack, time: Int): T {
        val experience = json.getFloatOrNull("experience")!!
        
        val recipe = recipeConstructor(key, result, input, experience, time)
        
        val category = json.getStringOrNull("category")
            ?.let { CookingBookCategory.valueOf(it.uppercase()) }
            ?: CookingBookCategory.MISC
        recipe.category = category
        
        return recipe
    }
    
}

internal object FurnaceRecipeDeserializer : CookingRecipeDeserializer<FurnaceRecipe>(::FurnaceRecipe)
internal object BlastingRecipeDeserializer : CookingRecipeDeserializer<BlastingRecipe>(::BlastingRecipe)
internal object SmokingRecipeDeserializer : CookingRecipeDeserializer<SmokingRecipe>(::SmokingRecipe)
internal object CampfireRecipeDeserializer : CookingRecipeDeserializer<CampfireRecipe>(::CampfireRecipe)
