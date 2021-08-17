package xyz.xenondevs.nova.data.config

import com.google.gson.JsonObject
import xyz.xenondevs.nova.IS_VERSION_CHANGE
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.recipe.*
import xyz.xenondevs.nova.util.data.*
import java.io.File

object NovaConfig : JsonConfig(File("plugins/Nova/config.json"), false) {
    
    private val DEFAULT_CONFIG_VALUES_FILE = File("plugins/Nova/defaultConfigValues.do-not-edit")
    
    private val defaultConfig = JsonConfig(DEFAULT_CONFIG_VALUES_FILE, false)
    private val internalConfig = JsonConfig(JSON_PARSER.parse(getResourceAsStream("config/config.json")!!.reader()).asJsonObject)
    
    fun init() {
        LOGGER.info("Loading config")
        extractNeededFiles()
        reload()
        defaultConfig.reload()
        
        updateUnchangedConfigValues()
        
        if (IS_VERSION_CHANGE) {
            set("resource_pack.url", internalConfig.getString("resource_pack.url")!!)
            save(true)
        }
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
        
        if (!DEFAULT_CONFIG_VALUES_FILE.exists()) File("plugins/Nova/config.json").copyTo(DEFAULT_CONFIG_VALUES_FILE)
    }
    
    private fun updateUnchangedConfigValues() {
        // loop over all elements of the internal config
        for ((key, internalElement) in internalConfig.config.entrySet()) {
            // get what's configured in the user config under that key
            val userConfiguredElement = config.get(key)
            
            // check if the configured element is different from the internal one
            if (internalElement != userConfiguredElement) {
                
                // if this key doesn't exist or doesn't differ from the originally extracted value
                // it's safe to replace it with the internal value
                if (userConfiguredElement == null || (defaultConfig.config.get(key) == userConfiguredElement)) {
                    config[key] = internalElement
                    
                    // also write it to the default config as this is now a default value
                    defaultConfig.config[key] = internalElement
                }
            }
        }
        
        // save changes
        save(true)
        defaultConfig.save(true)
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
                val element = file.reader().use { JSON_PARSER.parse(it) }
                if (element !is JsonObject)
                    throw IllegalStateException("Invalid recipe in file ${file.name}.")
                
                if (!element.getBoolean("enabled", default = true))
                    return@forEach
                
                val recipe = GSON.fromJson<T>(element)!!
                recipes += recipe
            } catch (ex: IllegalArgumentException) {
                throw IllegalStateException("Invalid recipe in file ${file.name}.", ex)
            }
        }
        
        return recipes
    }
    
    override fun get(path: List<String>) =
        super.get(path) ?: internalConfig.get(path)
    
}