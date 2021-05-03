package xyz.xenondevs.nova.config

import com.google.gson.JsonParser
import xyz.xenondevs.nova.recipe.NovaRecipe
import xyz.xenondevs.nova.util.*
import java.io.File

object NovaConfig : JsonConfig(File("plugins/Nova/config.json"), false) {
    
    private val fallbackConfig = JsonConfig(
        JsonParser.parseReader(getResourceAsStream("config/config.json")!!.reader()).asJsonObject
    )
    
    fun init() {
        extractNeededFiles()
        reload()
    }
    
    fun extractNeededFiles() {
        getResources("config/").forEach { entry ->
            val file = File("plugins/Nova/" + entry.drop(7))
            if (!file.exists()) {
                val parent = file.parentFile
                if (parent != null && !parent.exists()) parent.mkdirs()
                file.writeBytes(getResourceData(entry))
            }
        }
    }
    
    fun loadRecipes(): List<NovaRecipe> {
        val recipes = ArrayList<NovaRecipe>()
        val recipesDirectory = File("plugins/Nova/recipes")
        recipesDirectory.walkTopDown().filter(File::isFile).forEach { file ->
            try {
                val element = file.reader().use(JsonParser::parseReader)
                val recipe = GSON.fromJson<NovaRecipe>(element)!!
                recipes += recipe
            } catch (ex: IllegalArgumentException) {
                throw IllegalStateException("Invalid recipe in file ${file.name}.", ex)
            }
        }
        return recipes
    }
    
    override fun get(path: List<String>) =
        super.get(path) ?: fallbackConfig.get(path)
    
}