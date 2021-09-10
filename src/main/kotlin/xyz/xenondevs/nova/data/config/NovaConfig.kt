package xyz.xenondevs.nova.data.config

import xyz.xenondevs.nova.IS_VERSION_CHANGE
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.data.JSON_PARSER
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResourceData
import xyz.xenondevs.nova.util.data.set
import java.io.File

object NovaConfig : JsonConfig(File("plugins/Nova/config.json"), false) {
    
    private val DEFAULT_CONFIG_VALUES_FILE = File("plugins/Nova/defaultConfigValues.do-not-edit")
    
    private val defaultConfig = JsonConfig(DEFAULT_CONFIG_VALUES_FILE, false)
    private val internalConfig = JsonConfig(JSON_PARSER.parse(getResourceAsStream("config/config.json")!!.reader()).asJsonObject)
    
    fun init() {
        LOGGER.info("Loading config")
        extractConfigFiles()
        reload()
        defaultConfig.reload()
        
        updateUnchangedConfigValues()
        
        if (IS_VERSION_CHANGE) {
            set("resource_pack.url", internalConfig.getString("resource_pack.url")!!)
            save(true)
        }
    }
    
    private fun extractConfigFiles() {
        if (!file!!.exists()) file.writeBytes(getResourceData("config/config.json"))
        if (!DEFAULT_CONFIG_VALUES_FILE.exists()) file.copyTo(DEFAULT_CONFIG_VALUES_FILE)
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
    
    override fun get(path: List<String>) =
        super.get(path) ?: internalConfig.get(path)
    
}