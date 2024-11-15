package xyz.xenondevs.nova.world.item.recipe

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getAllJsonObjects
import xyz.xenondevs.commons.gson.getBooleanOrNull
import xyz.xenondevs.commons.gson.hasArray
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import java.io.File
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

internal object RecipesLoader {
    
    fun extractAndLoadRecipes(): List<Any> {
        UpdatableFile.extractIdNamedFromAllAddons("recipes")
        return loadRecipes()
    }
    
    private fun loadRecipes(): List<Any> {
        return RECIPE_TYPE.flatMap {
            val deserializer = it.deserializer
            if (deserializer != null) {
                loadRecipes(it.dirName, it.deserializer)
            } else emptyList()
        }
    }
    
    private fun <T : Any> loadRecipes(folder: String, deserializer: RecipeDeserializer<T>): List<T> {
        return AddonBootstrapper.addons.flatMap { addon ->
            val recipesDirectory = addon.dataFolder.resolve("recipes/$folder")
            recipesDirectory.walk()
                .filter { it.isRegularFile() && ResourcePath.isValidPath(it.name) }
                .mapNotNullTo(ArrayList()) { loadRecipe(it.toFile(), deserializer) }
        }
    }
    
    private fun <T : Any> loadRecipe(file: File, deserializer: RecipeDeserializer<T>): T? {
        var failSilently = false
        val fallbacks = when (val element = file.parseJson()) {
            is JsonArray -> element.getAllJsonObjects()
            is JsonObject -> if (element.hasArray("recipes")) {
                failSilently = element.getBooleanOrNull("failSilently") ?: false
                element.getAsJsonArray("recipes").getAllJsonObjects()
            } else {
                failSilently = element.getBooleanOrNull("failSilently") ?: false
                listOf(element)
            }
            
            else -> null
        }
        
        if (!fallbacks.isNullOrEmpty()) {
            // store exceptions in case no fallback works
            val exceptions = ArrayList<Exception>()
            
            fallbacks.forEach { obj ->
                try {
                    return deserializer.deserialize(obj, file)
                } catch (e: Exception) {
                    exceptions += e
                }
            }
            
            // Log exceptions if all fallbacks failed
            if (!failSilently)
                exceptions.forEachIndexed { i, e -> LOGGER.error("Could not load recipe in file $file (recipe fallback $i)", e) }
        } else {
            LOGGER.error("Invalid recipe file $file: Recipe is neither a json object nor an array of json objects")
        }
        
        return null
    }
    
}