package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.data.copy
import java.io.ByteArrayInputStream
import java.io.File

object ConfigExtractor {
    
    private val storedConfigs: HashMap<String, YamlConfiguration> = PermanentStorage.retrieve("storedConfigs", ::HashMap)
    
    init {
        NOVA.disableHandlers += { PermanentStorage.store("storedConfigs", storedConfigs) }
    }
    
    fun extract(configPath: String, data: ByteArray): YamlConfiguration {
        val file = File(NOVA.dataFolder, configPath)
        val internalCfg = YamlConfiguration.loadConfiguration(ByteArrayInputStream(data).reader())
        val storedCfg = storedConfigs[configPath]
        
        val cfg: YamlConfiguration
        if (!file.exists() || storedCfg == null) {
            cfg = internalCfg.copy()
            storedConfigs[configPath] = internalCfg.copy()
        } else cfg = updateExistingConfig(file, storedCfg, internalCfg)
        
        cfg.save(file)
        cfg.setDefaults(internalCfg)
        
        return cfg
    }
    
    private fun updateExistingConfig(file: File, storedCfg: YamlConfiguration, internalCfg: YamlConfiguration): YamlConfiguration {
        val cfg = YamlConfiguration.loadConfiguration(file)
        
        // add keys that are new
        internalCfg.getKeys(true).filterNot(cfg::isSet).forEach { path ->
            val internalValue = internalCfg.get(path)
            cfg.set(path, internalValue)
            cfg.setComments(path, internalCfg.getComments(path))
            cfg.setInlineComments(path, internalCfg.getInlineComments(path))
            storedCfg.set(path, internalValue)
        }
        
        // update keys that were unchanged by the user
        internalCfg.getKeys(true).forEach { path ->
            val internalValue = internalCfg.get(path)
            if (internalValue is ConfigurationSection)
                return@forEach
            
            val storedValue = storedCfg.get(path)
            val configuredValue = cfg.get(path)
            
            // check that the configured value differs from the internal value but was not changed by the user
            if (internalValue != configuredValue && (storedValue == configuredValue ||
                    (storedValue is List<*> && configuredValue is List<*> && storedValue.contentEquals(configuredValue)))
            ) {
                cfg.set(path, internalValue)
                cfg.setComments(path, internalCfg.getComments(path))
                cfg.setInlineComments(path, internalCfg.getInlineComments(path))
                storedCfg.set(path, internalValue)
            }
        }
        
        return cfg
    }
    
}