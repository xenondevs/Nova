package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.JsonObject
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.*
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

private fun getRecipeKey(recipeType: String, file: File): NamespacedKey =
    NamespacedKey(NOVA, "nova.$recipeType.${file.name}")

interface NovaRecipeDeserializer<T : NovaRecipe> {
    
    fun deserialize(json: JsonObject, file: File): T
    
}

object ShapedNovaRecipeDeserializer : NovaRecipeDeserializer<ShapedNovaRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapedNovaRecipe {
        val resultKey = json.getString("result")!!
        
        val resultBuilder = getItemBuilder(resultKey)
        resultBuilder.amount = json.getInt("amount", default = 1)
        
        val shape = json.getAsJsonArray("shape").toStringList(::ArrayList)
        val ingredients = HashMap<Char, RecipeChoice>()
        json.getAsJsonArray("ingredients").filterIsInstance<JsonObject>().forEach { ingredient ->
            val char = ingredient.getString("char")!!.first()
            val recipeChoice = MaterialUtils.getRecipeChoice(ingredient.getString("item")!!)
            ingredients[char] = recipeChoice
        }
        
        return ShapedNovaRecipe(getRecipeKey("shaped", file), resultBuilder, shape, ingredients)
    }
    
}

object ShapelessNovaRecipeDeserializer : NovaRecipeDeserializer<ShapelessNovaRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): ShapelessNovaRecipe {
        val resultKey = json.getString("result")!!
        
        val resultBuilder = getItemBuilder(resultKey)
        resultBuilder.amount = json.getInt("amount", default = 1)
        
        val ingredients = json.getAsJsonObject("ingredients")
            .entrySet()
            .associate { (key, value) ->
                MaterialUtils.getRecipeChoice(key) to value.asInt
            }
        
        return ShapelessNovaRecipe(getRecipeKey("shapeless", file), resultBuilder, ingredients)
    }
    
}

object FurnaceNovaRecipeDeserializer : NovaRecipeDeserializer<FurnaceNovaRecipe> {
    
    override fun deserialize(json: JsonObject, file: File): FurnaceNovaRecipe {
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        val input = MaterialUtils.getRecipeChoice(json.getString("input")!!)
        val experience = json.getFloat("experience")!!
        val cookingTime = json.getInt("cookingTime")!!
        
        return FurnaceNovaRecipe(getRecipeKey("furnace", file), input, result, experience, cookingTime)
    }
    
}

abstract class CustomNovaRecipeDeserializer<T : CustomNovaRecipe>(
    private val type: String,
    private val constructor: (NamespacedKey, List<ItemBuilder>, ItemBuilder, Int) -> T
) : NovaRecipeDeserializer<T> {
    
    override fun deserialize(json: JsonObject, file: File): T {
        val input = if (json.hasString("input")) {
            listOf(getItemBuilder(json.getString("input")!!))
        } else {
            json.getAsJsonArray("input")
                .getAllStrings()
                .map(::getItemBuilder)
        }
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        val time = json.getInt("time")!!
        
        return constructor(getRecipeKey(type, file), input, result, time)
    }
    
}

object PulverizerNovaRecipeDeserializer : CustomNovaRecipeDeserializer<PulverizerNovaRecipe>("pulverizer", ::PulverizerNovaRecipe)

object PlatePressNovaRecipeDeserializer : CustomNovaRecipeDeserializer<PlatePressNovaRecipe>("plate_press", ::PlatePressNovaRecipe)

object GearPressNovaRecipeDeserializer : CustomNovaRecipeDeserializer<GearPressNovaRecipe>("gear_press", ::GearPressNovaRecipe)
