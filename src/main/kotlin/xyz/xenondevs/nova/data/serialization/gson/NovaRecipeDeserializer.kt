package xyz.xenondevs.nova.data.serialization.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.*
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.MaterialUtils
import xyz.xenondevs.nova.util.data.*
import java.lang.reflect.Type

@Suppress("LiftReturnOrAssignment", "CascadeIf")
private fun getItemBuilder(name: String): ItemBuilder {
    if (name.startsWith("nova:")) {
        return NovaMaterialRegistry.get(name.substringAfter(':').uppercase()).createItemBuilder()
    } else if (name.startsWith("minecraft:")) {
        return ItemBuilder(Material.valueOf(name.substringAfter(':').uppercase()))
    } else throw IllegalArgumentException("Invalid item name: $name")
}

object ShapedNovaRecipeDeserializer : JsonDeserializer<ShapedNovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ShapedNovaRecipe {
        json as JsonObject
        
        val resultKey = json.getString("result")!!
        val name = json.getString("name") ?: resultKey.split(":")[1]
        
        val resultBuilder = getItemBuilder(resultKey)
        resultBuilder.amount = json.getInt("amount", default = 1)
        
        val shape = json.getAsJsonArray("shape").toStringList(::ArrayList)
        val ingredients = HashMap<Char, RecipeChoice>()
        json.getAsJsonArray("ingredients").filterIsInstance<JsonObject>().forEach { ingredient ->
            val char = ingredient.getString("char")!!.first()
            val recipeChoice = MaterialUtils.getRecipeChoice(ingredient.getString("item")!!)
            ingredients[char] = recipeChoice
        }
        
        return ShapedNovaRecipe(NamespacedKey(NOVA, "shaped_$name"), resultBuilder, shape, ingredients)
    }
    
}

object ShapelessNovaRecipeDeserializer : JsonDeserializer<ShapelessNovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ShapelessNovaRecipe {
        json as JsonObject
        
        val resultKey = json.getString("result")!!
        val name = json.getString("name") ?: resultKey.split(":")[1]
        
        val resultBuilder = getItemBuilder(resultKey)
        resultBuilder.amount = json.getInt("amount", default = 1)
        
        val ingredients = json.getAsJsonObject("ingredients")
            .entrySet()
            .associate { (key, value) ->
                MaterialUtils.getRecipeChoice(key) to value.asInt
            }
        
        return ShapelessNovaRecipe(NamespacedKey(NOVA, "shapeless_$name"), resultBuilder, ingredients)
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
    private val constructor: (List<ItemBuilder>, ItemBuilder) -> T
) : JsonDeserializer<T> {
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
        json as JsonObject
        
        val input = if (json.hasString("input")) {
            listOf(getItemBuilder(json.getString("input")!!))
        } else {
            json.getAsJsonArray("input")
                .getAllStrings()
                .map(::getItemBuilder)
        }
        
        val result = getItemBuilder(json.getString("result")!!)
        result.amount = json.getInt("amount", default = 1)
        
        return constructor(input, result)
    }
    
}

object PulverizerNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<PulverizerNovaRecipe>(::PulverizerNovaRecipe)

object PlatePressNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<PlatePressNovaRecipe>(::PlatePressNovaRecipe)

object GearPressNovaRecipeDeserializer : ConversionNovaRecipeDeserializer<GearPressNovaRecipe>(::GearPressNovaRecipe)
