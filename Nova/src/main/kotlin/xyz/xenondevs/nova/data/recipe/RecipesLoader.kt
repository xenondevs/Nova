package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.loader.AddonManager
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
import xyz.xenondevs.nova.util.data.*
import java.io.File
import java.io.InputStream

object RecipesLoader {
    
    private val recipesDir = File(NOVA.dataFolder, "recipes/")
    private val fileHashes: HashMap<String, String> = PermanentStorage.retrieve("recipeFileHashes") { HashMap() }
    
    init {
        extractRecipes()
        PermanentStorage.store("recipeFileHashes", fileHashes)
    }
    
    private fun extractRecipes() {
        val existingPaths = ArrayList<String>()
        
        // Extract core recipes
        getResources("recipes/").forEach { path ->
            existingPaths += path
            extractRecipe(path) { getResourceAsStream(path)!! }
        }
        
        // Extract recipes from addons
        AddonManager.loaders.forEach { loader ->
            getResources(loader.file, "recipes/").forEach { path ->
                existingPaths += path
                extractRecipe(path) { loader.classLoader.getResourceAsStream(path)!! }
            }
        }
        
        // find unedited recipe files that are no longer default and remove them
        recipesDir.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val relativePath = NOVA.dataFolder.toURI().relativize(file.toURI()).path
            
            if (!existingPaths.contains(relativePath)
                && HashUtils.getFileHash(file, "MD5").contentEquals(getFileHash(file))) {
                
                fileHashes.remove(file.absolutePath)
                file.delete()
            }
        }
    }
    
    private fun extractRecipe(path: String, getStream: () -> InputStream) {
        val recipeFile = File(NOVA.dataFolder, path)
        val savedHash = getFileHash(recipeFile)
        
        if (recipeFile.exists() && savedHash != null) {
            val recipeFileHash = HashUtils.getFileHash(recipeFile, "MD5")
            if (recipeFileHash.contentEquals(savedHash)) {
                getStream().copyTo(recipeFile.outputStream())
                storeFileHash(recipeFile)
            }
        } else if (savedHash == null) {
            recipeFile.parentFile.mkdirs()
            getStream().copyTo(recipeFile.outputStream())
            storeFileHash(recipeFile)
        }
    }
    
    private fun storeFileHash(originalFile: File) {
        fileHashes[originalFile.absolutePath] = HashUtils.getFileHash(originalFile, "MD5").encodeWithBase64()
    }
    
    private fun getFileHash(originalFile: File): ByteArray? =
        fileHashes[originalFile.absolutePath]?.decodeWithBase64()
    
    fun loadRecipes(): List<Any> {
        return RecipeTypeRegistry.types.flatMap {
            val dirName = it.dirName
            val deserializer = it.deserializer
            if (dirName != null && deserializer != null) {
                loadRecipes(it.dirName, it.deserializer)
            } else emptyList()
        }
    }
    
    private fun loadRecipes(folder: String, deserializer: RecipeDeserializer<out Any>): List<Any> {
        val recipes = ArrayList<Any>()
        
        val recipesDirectory = File("plugins/Nova/recipes/$folder")
        recipesDirectory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".json") }
            .forEach { file ->
                try {
                    val element = file.reader().use { JsonParser.parseReader(it) }
                    if (element !is JsonObject)
                        throw IllegalStateException("Invalid recipe in file ${file.name}.")
                    
                    if (!element.getBoolean("enabled", default = true))
                        return@forEach
                    
                    val recipe = deserializer.deserialize(element, file)
                    recipes += recipe
                } catch (ex: Exception) {
                    throw IllegalStateException("Invalid recipe in file ${file.name}.", ex)
                }
            }
        
        return recipes
    }
    
}