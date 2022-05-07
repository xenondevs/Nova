package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import java.io.File
import java.util.logging.Level

object RecipesLoader {
    
    private val RECIPE_FILE_PATTERN = Regex("""^[a-z][a-z0-9_]*.json$""")
    
    private val recipesDir = File(NOVA.dataFolder, "recipes/")
    
    init {
        extractRecipes()
    }
    
    private fun extractRecipes() {
        val existingPaths = ArrayList<String>()
        
        // Extract core recipes
        getResources("recipes/").forEach { path ->
            existingPaths += path
            UpdatableFile.load(File(NOVA.dataFolder, path)) { getResourceAsStream(path)!! }
        }
        
        // Extract recipes from addons
        AddonManager.loaders.values.forEach { loader ->
            getResources(loader.file, "recipes/").forEach { path ->
                existingPaths += path
                UpdatableFile.load(File(NOVA.dataFolder, path)) { getResourceAsStream(loader.file, path)!! }
            }
        }
        
        // find unedited recipe files that are no longer default and remove them
        recipesDir.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val relativePath = NOVA.dataFolder.toURI().relativize(file.toURI()).path
            
            if (!existingPaths.contains(relativePath)
                && HashUtils.getFileHash(file, "MD5").contentEquals(UpdatableFile.getStoredHash(file))) {
                
                UpdatableFile.removeStoredHash(file)
                file.delete()
            }
        }
    }
    
    fun loadRecipes(): List<Any> {
        return RecipeTypeRegistry.types.flatMap {
            val dirName = it.dirName
            val deserializer = it.deserializer
            if (dirName != null && deserializer != null) {
                loadRecipes(it.dirName, it.deserializer)
            } else emptyList()
        }
    }
    
    private fun <T : Any> loadRecipes(folder: String, deserializer: RecipeDeserializer<T>): List<T> {
        val recipesDirectory = File("plugins/Nova/recipes/$folder")
        return recipesDirectory.walkTopDown()
            .filter { it.isFile && it.name.matches(RECIPE_FILE_PATTERN) }
            .mapNotNullTo(ArrayList()) { loadRecipe(it, deserializer) }
    }
    
    private fun <T : Any> loadRecipe(file: File, deserializer: RecipeDeserializer<T>): T? {
        try {
            var recipeFile = file
            var fallback = 0
            
            while (recipeFile.exists()) {
                try {
                    val element = recipeFile.reader().use { JsonParser.parseReader(it) }
                    if (element !is JsonObject)
                        throw IllegalArgumentException("Recipe is not a json object")
                    
                    return deserializer.deserialize(element, recipeFile)
                } catch (ex: Exception) {
                    fallback++
                    recipeFile = File(file.parentFile, "${file.nameWithoutExtension}-$fallback.json")
                    
                    if (!recipeFile.exists())
                        throw IllegalStateException("Invalid recipe in file ${file.name} (fallback ${fallback - 1}).", ex)
                }
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Invalid recipe in $file", e)
        }
        
        return null
    }
    
}