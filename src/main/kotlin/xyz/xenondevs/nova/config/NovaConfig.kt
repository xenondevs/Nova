package xyz.xenondevs.nova.config

import com.google.gson.JsonParser
import xyz.xenondevs.nova.recipe.*
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
    
    private fun extractNeededFiles() {
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
        recipes += loadRecipes<ShapedNovaRecipe>("shaped")
        recipes += loadRecipes<ShapelessNovaRecipe>("shapeless")
        recipes += loadRecipes<FurnaceNovaRecipe>("furnace")
        recipes += loadRecipes<PulverizerNovaRecipe>("pulverizer")
        recipes += loadRecipes<PlatePressNovaRecipe>("press/plate")
        recipes += loadRecipes<GearPressNovaRecipe>("press/gear")
        return recipes
    }
    
    private inline fun <reified T> loadRecipes(folder: String): List<NovaRecipe> where T : NovaRecipe {
        val recipes = ArrayList<NovaRecipe>()
        
        val recipesDirectory = File("plugins/Nova/recipes/$folder")
        recipesDirectory.walkTopDown().filter(File::isFile).forEach { file ->
            try {
                val element = file.reader().use(JsonParser::parseReader)
                val recipe = GSON.fromJson<T>(element)!!
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