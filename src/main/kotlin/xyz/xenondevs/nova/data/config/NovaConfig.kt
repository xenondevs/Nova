package xyz.xenondevs.nova.data.config

import xyz.xenondevs.nova.IS_VERSION_CHANGE
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.data.*
import java.io.File

val DEFAULT_CONFIG = NovaConfig["config"]

class NovaConfig(private val configPath: String) : JsonConfig(File("${NOVA.dataFolder}/$configPath"), false) {
    
    private val defaultConfigValuesFile = File(file!!.parentFile, ".${file.nameWithoutExtension}.defaults")
    
    private val defaultConfig = JsonConfig(defaultConfigValuesFile, false)
    private val internalConfig = JsonConfig(JSON_PARSER.parse(getResourceAsStream(configPath)!!.reader()).asJsonObject)
    
    init {
        extractConfigFiles()
        reload()
        defaultConfig.reload()
        
        updateUnchangedConfigValues()
    }
    
    private fun extractConfigFiles() {
        file!!.parentFile.mkdirs()
        if (!file.exists()) file.writeBytes(getResourceData(configPath))
        if (!defaultConfigValuesFile.exists()) file.copyTo(defaultConfigValuesFile)
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
    
    companion object {
        
        private val configs = HashMap<String, NovaConfig>()
        
        fun init() {
            LOGGER.info("Loading configs")
            
            getResources("config/")
                .filterNot { it.startsWith("config/recipes/") }
                .forEach {
                    val configName = it.substring(7).substringBeforeLast('.')
                    configs[configName] = NovaConfig(it)
                }
            
            if (IS_VERSION_CHANGE) {
                val defaultConfig = configs["config"]!!
                defaultConfig["resource_pack.url"] = defaultConfig.internalConfig.getString("resource_pack.url")!!
                defaultConfig.save(true)
            }
        }
        
        operator fun get(name: String) = configs[name]!!
        
        operator fun get(material: NovaMaterial) = configs["machine/${material.typeName.lowercase()}"]!!
        
    }
    
}