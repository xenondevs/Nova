package xyz.xenondevs.nova.serialization

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.NovaRecipe
import xyz.xenondevs.nova.util.MaterialUtils
import xyz.xenondevs.nova.util.getInt
import xyz.xenondevs.nova.util.getString
import xyz.xenondevs.nova.util.toStringList
import java.lang.reflect.Type

object NovaRecipeDeserializer : JsonDeserializer<NovaRecipe> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): NovaRecipe {
        json as JsonObject
        
        val resultName = json.getString("result")!!.substringAfter(':').uppercase()
        println(resultName)
        val result = NovaMaterial.valueOf(resultName)
        val amount = json.getInt("amount", default = 1)!!
        val shape = json.getAsJsonArray("shape").toStringList(::ArrayList)

        val ingredients = HashMap<Char, RecipeChoice>()
        json.getAsJsonArray("ingredients").filterIsInstance<JsonObject>().forEach { ingr ->
            val char = ingr.getString("char")!!.first()
            val recipeChoice = MaterialUtils.getRecipeChoice(ingr.getString("item")!!)
            ingredients[char] = recipeChoice
        }
        
        return NovaRecipe(result, shape, ingredients, amount)
    }
    
}