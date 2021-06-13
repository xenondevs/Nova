package xyz.xenondevs.nova.serialization.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.*
import xyz.xenondevs.nova.util.*
import java.lang.IllegalArgumentException
import java.lang.reflect.Type

@Suppress("LiftReturnOrAssignment", "CascadeIf")
private fun getItemBuilder(name: String): ItemBuilder {
    if (name.startsWith("nova:")) {
        return NovaMaterial.valueOf(name.substringAfter(':').uppercase()).createItemBuilder()
    } else if (name.startsWith("minecraft:")) {
        return ItemBuilder(Material.valueOf(name.substringAfter(':').uppercase()))
    } else throw IllegalArgumentException("Invalid item name: $name")
}

object ShapedNovaRecipeDeserializer : JsonDeserializer<ShapedNovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ShapedNovaRecipe {
        json as JsonObject
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        val shape = json.getAsJsonArray("shape").toStringList(::ArrayList)
        
        val ingredients = HashMap<Char, RecipeChoice>()
        json.getAsJsonArray("ingredients").filterIsInstance<JsonObject>().forEach { ingredient ->
            val char = ingredient.getString("char")!!.first()
            val recipeChoice = MaterialUtils.getRecipeChoice(ingredient.getString("item")!!)
            ingredients[char] = recipeChoice
        }
        
        return ShapedNovaRecipe(result, shape, ingredients)
    }
    
}

object ShapelessNovaRecipeDeserializer : JsonDeserializer<ShapelessNovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ShapelessNovaRecipe {
        json as JsonObject
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        val ingredients = json.getAsJsonObject("ingredients")
            .entrySet()
            .associate { (key, value) ->
                MaterialUtils.getRecipeChoice(key) to value.asInt
            }
        
        return ShapelessNovaRecipe(result, ingredients)
    }
    
}

object FurnaceNovaRecipeDeserializer : JsonDeserializer<FurnaceNovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FurnaceNovaRecipe {
        json as JsonObject
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        val input = MaterialUtils.getRecipeChoice(json.getString("input")!!)
        val experience = json.getFloat("experience")!!
        val cookingTime = json.getInt("cookingTime")!!
        
        return FurnaceNovaRecipe(input, result, experience, cookingTime)
    }
    
}

abstract class ConversionNovaRecipeDeserializer<T : ConversionNovaRecipe>(
    private val constructor: (ItemBuilder, ItemBuilder) -> T
) : JsonDeserializer<T> {
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
        json as JsonObject
    
        val input = getItemBuilder(json.getString("input")!!)
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        
        return constructor(input, result)
    }
    
}

object PulverizerNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<PulverizerNovaRecipe>(::PulverizerNovaRecipe)

object PlatePressNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<PlatePressNovaRecipe>(::PlatePressNovaRecipe)

object GearPressNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<GearPressNovaRecipe>(::GearPressNovaRecipe)
