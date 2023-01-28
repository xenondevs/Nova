package xyz.xenondevs.nova.data.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import xyz.xenondevs.commons.collections.contentEquals
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.copy
import java.io.ByteArrayInputStream
import java.io.File

internal object ConfigExtractor {
    
    private val storedConfigs: HashMap<String, YamlConfiguration> = PermanentStorage.retrieve("storedConfigs", ::HashMap)
    
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
        
        // remove keys that were once extracted but are no longer in the internal config
        cfg.getKeys(true)
            .filter { !internalCfg.isSet(it) && storedCfg.isSet(it) }
            .forEach { path ->
                cfg.set(path, null)
                storedCfg.set(path, null)
            }
        
        internalCfg.getKeys(true).forEach { path ->
            // reset comments
            cfg.setComments(path, internalCfg.getComments(path))
            cfg.setInlineComments(path, internalCfg.getInlineComments(path))
            
            // update keys that were unchanged by the user
            val internalValue = internalCfg.get(path)
            if (internalValue is ConfigurationSection)
                return@forEach
            
            val storedValue = storedCfg.get(path)
            val configuredValue = cfg.get(path)
            
            if (checkNoUserChanges(internalValue, configuredValue, storedValue)) {
                cfg.set(path, internalValue)
                storedCfg.set(path, internalValue)
            }
        }
        
        return cfg
    }
    
    /**
     * Checks if the [configuredValue] differs from the [internalValue] but was not changed by the user by cross-referencing it with [storedValue].
     */
    private fun checkNoUserChanges(internalValue: Any?, configuredValue: Any?, storedValue: Any?): Boolean =
        internalValue != configuredValue && checkEquality(storedValue, configuredValue)
    
    /**
     * Checks if [value1] and [value2] are equal or if they're lists and have the same content.
     */
    private fun checkEquality(value1: Any?, value2: Any?): Boolean =
        value1 == value2 || (value1 is List<*> && value2 is List<*> && value1.contentEquals(value2))
    
    internal fun saveStoredConfigs() {
        PermanentStorage.store("storedConfigs", storedConfigs)
    }
    
}