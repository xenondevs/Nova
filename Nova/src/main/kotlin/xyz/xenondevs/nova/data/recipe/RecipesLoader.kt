package xyz.xenondevs.nova.data.recipe

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
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
                if (recipeFileHash.contentEquals(savedHash)) {
                    recipeFile.writeBytes(getResourceData(entry))
                    storeFileHash(recipeFile)
                }
            } else if (savedHash == null) {
                recipeFile.parentFile.mkdirs()
                recipeFile.writeBytes(getResourceData(entry))
                storeFileHash(recipeFile)
            }
        }
        
        // find unedited recipe files that are no longer default and remove them
        val recipesDirectory = File(NOVA.dataFolder, "recipes/")
        recipesDirectory.walkTopDown().forEach { file ->
            if (file.isDirectory) return@forEach
            
            val pathInZip = "config/recipes/" + file.absolutePath.substring(recipesDirectory.absolutePath.length + 1).replace(File.separatorChar, '/') // TODO: clean up
            if (!hasResource(pathInZip)
                && HashUtils.getFileHash(file, "MD5").contentEquals(getFileHash(file))) {
                
                fileHashes.remove(file.absolutePath)
                file.delete()
            }
        }
    }
    
    private fun storeFileHash(originalFile: File) {
        fileHashes[originalFile.absolutePath] = HashUtils.getFileHash(originalFile, "MD5").encodeWithBase64()
    }
    
    private fun getFileHash(originalFile: File): ByteArray? =
        fileHashes[originalFile.absolutePath]?.decodeWithBase64()
    
    fun loadRecipes(): List<Any> {
        return RecipeType.values.flatMap {
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
                } catch (ex: IllegalArgumentException) {
                    throw IllegalStateException("Invalid recipe in file ${file.name}.", ex)
                }
            }
        
        return recipes
    }
    
}