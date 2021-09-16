package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonObject
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.json.*
import xyz.xenondevs.nova.util.data.*
import java.io.File

object RecipesLoader {
    
    init {
        extractRecipes()
    }
    
    private fun extractRecipes() {
        getResources("config/recipes").forEach { entry ->
            val recipeFile = File(NOVA.dataFolder, "recipes/${entry.substring(14)}")
            val hashFile = getHashFile(recipeFile)
            
            if (recipeFile.exists() && hashFile.exists()) {
                val recipeFileHash = HashUtils.getFileHash(recipeFile, "MD5")
                val savedHash = hashFile.readBytes()
                if (recipeFileHash.contentEquals(savedHash))
                    recipeFile.writeBytes(getResourceData(entry))
            } else {
                recipeFile.parentFile.mkdirs()
                recipeFile.writeBytes(getResourceData(entry))
                hashFile.writeBytes(HashUtils.getFileHash(recipeFile, "MD5"))
            }
        }
    }
    
    private fun getHashFile(originalFile: File): File =
        File(originalFile.parent, ".${originalFile.name}.md5")
    
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