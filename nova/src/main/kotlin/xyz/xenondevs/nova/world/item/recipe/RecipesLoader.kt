package xyz.xenondevs.nova.world.item.recipe

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.commons.gson.getAllJsonObjects
import xyz.xenondevs.commons.gson.getBooleanOrNull
import xyz.xenondevs.commons.gson.hasArray
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.util.data.UpdatableFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.extension

internal object RecipesLoader {
    
    suspend fun extractAndLoadRecipes(): List<Any> {
        val files = UpdatableFile.extractIdNamedFromAllAddons("recipes") { it.extension == "json" }
        return RECIPE_TYPE.entrySet.get()
            .flatMap { recipeType ->
                recipeType.deserializer
                    ?.let { loadRecipes(recipeType.dirName, it, files) }
                    ?: []
            }
    }
    
    private fun <T : Any> loadRecipes(folder: String, deserializer: RecipeDeserializer<T>, files: Map<Path, ByteArray>): List<T> {
        val directories = AddonBootstrapper.addons.map { it.dataFolder.resolve("recipes/$folder") }
        return files.entries.asSequence()
            .filter { [file, _] -> directories.any(file::startsWith) }
            .mapNotNull { [file, data] -> loadRecipe(file.toFile(), data.decodeToString(), deserializer) }
            .toList()
    }
    
    private fun <T : Any> loadRecipe(file: File, json: String, deserializer: RecipeDeserializer<T>): T? {
        var failSilently = false
        val fallbacks = when (val element = JsonParser.parseString(json)) {
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