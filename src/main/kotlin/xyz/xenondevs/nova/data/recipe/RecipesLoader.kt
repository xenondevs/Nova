package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonObject
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.json.*
import xyz.xenondevs.nova.util.data.*
import java.io.File

object RecipesLoader {
    
    private val fileHashes: HashMap<String, String> = PermanentStorage.retrieve("recipeFileHashes") { HashMap() }
    
    init {
        extractRecipes()
        PermanentStorage.store("recipeFileHashes", fileHashes)
    }
    
    private fun extractRecipes() {
        getResources("config/recipes").forEach { entry ->
            val recipeFile = File(NOVA.dataFolder, "recipes/${entry.substring(14)}")
            val savedHash = getFileHash(recipeFile)
            
            if (recipeFile.exists() && savedHash != null) {
                val recipeFileHash = HashUtils.getFileHash(recipeFile, "MD5")
                if (recipeFileHash.contentEquals(savedHash))
                    recipeFile.writeBytes(getResourceData(entry))
            } else if (savedHash == null) {
                recipeFile.parentFile.mkdirs()
                recipeFile.writeBytes(getResourceData(entry))
                storeFileHash(recipeFile)
            }
        }
    }
    
    private fun storeFileHash(originalFile: File) {
        fileHashes[originalFile.absolutePath] = HashUtils.getFileHash(originalFile, "MD5").encodeWithBase64()
    }
    
    private fun getFileHash(originalFile: File): ByteArray? =
        fileHashes[originalFile.absolutePath]?.decodeWithBase64()
    
    fun loadRecipes(): List<NovaRecipe> {
        val recipes = ArrayList<NovaRecipe>()
        
        recipes += loadRecipes("shaped", ShapedNovaRecipeDeserializer)
        recipes += loadRecipes("shapeless", ShapelessNovaRecipeDeserializer)
        recipes += loadRecipes("furnace", FurnaceNovaRecipeDeserializer)
        recipes += loadRecipes("pulverizer", PulverizerNovaRecipeDeserializer)
        recipes += loadRecipes("press/plate", PlatePressNovaRecipeDeserializer)
        recipes += loadRecipes("press/gear", GearPressNovaRecipeDeserializer)
        return recipes
    }
    
    private fun loadRecipes(folder: String, deserializer: NovaRecipeDeserializer<out NovaRecipe>): List<NovaRecipe> {
        val recipes = ArrayList<NovaRecipe>()
        
        val recipesDirectory = File("plugins/Nova/recipes/$folder")
        recipesDirectory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".json") }
            .forEach { file ->
                try {
                    val element = file.reader().use { JSON_PARSER.parse(it) }
                    if (element !is JsonObject)
                        throw IllegalStateException("Invalid recipe in file ${file.name}.")
                    
                    if (!element.getBoolean("enabled", default = true))
                        return@forEach
                    
                    val recipe = deserializer.deserialize(element, file)
                    recipes += recipe
                } catch (ex: IllegalArgumentException) {
                    throw IllegalStateException("Invalid recipe in file ${file.name}.", ex)
                }
            }
        
        return recipes
    }
    
}