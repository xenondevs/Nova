package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
import xyz.xenondevs.nova.util.data.*
import java.io.File
import java.util.logging.Level

object RecipesLoader {
    
    private val RECIPE_FILE_PATTERN = Regex("""^[a-z][a-z\d_]*.json$""")
    
    private val recipesDir = File(NOVA.dataFolder, "recipes/")
    
    init {
        extractRecipes()
    }
    
    private fun extractRecipes() {
        val existingPaths = ArrayList<String>()
        
        // Extract core recipes
        existingPaths += getResources("recipes/").mapNotNull(::extractRecipe)
        
        // Extract recipes from addons
        AddonManager.loaders.values.forEach { loader ->
            existingPaths += getResources(loader.file, "recipes/").mapNotNull { extractRecipe(it, loader) }
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
    
    private fun extractRecipe(path: String, addon: AddonLoader? = null): String? {
        val namespace = addon?.description?.id ?: "nova"
        val file = File(NOVA.dataFolder, path).let { File(it.parent, namespace + "_" + it.name) }
        if (file.name.matches(RECIPE_FILE_PATTERN)) {
            UpdatableFile.load(file) { if (addon != null) getResourceAsStream(addon.file, path)!! else getResourceAsStream(path)!! }
            return NOVA.dataFolder.toURI().relativize(file.toURI()).path
        }
        
        LOGGER.severe("Could not load recipe file $path: Invalid file name")
        return null
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
        var failSilently = false
        val fallbacks = when (val element = file.reader().use(JsonParser::parseReader)) {
            is JsonArray -> element.getAllJsonObjects()
            is JsonObject -> if (element.hasArray("recipes")) {
                failSilently = element.getBoolean("failSilently", false)
                element.getAsJsonArray("recipes").getAllJsonObjects()
            } else {
                failSilently = element.getBoolean("failSilently", false)
                listOf(element)
            }
            else -> null
        }
        
        if (fallbacks != null && fallbacks.isNotEmpty()) {
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
                exceptions.forEachIndexed { i, e -> LOGGER.log(Level.SEVERE, "Could not load recipe in file $file (recipe fallback $i)", e) }
        } else {
            LOGGER.log(Level.SEVERE, "Invalid recipe file $file: Recipe is neither a json object nor an array of json objects")
        }
        
        return null
    }
    
}