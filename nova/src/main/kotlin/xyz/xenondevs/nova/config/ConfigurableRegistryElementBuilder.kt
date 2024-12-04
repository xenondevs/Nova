package xyz.xenondevs.nova.config

import net.kyori.adventure.key.Key
import net.minecraft.core.WritableRegistry
import xyz.xenondevs.nova.registry.RegistryElementBuilder

abstract class ConfigurableRegistryElementBuilder<T : Any>(
    registry: WritableRegistry<in T>,
    id: Key
) : RegistryElementBuilder<T>(registry, id) {
    
    protected var configId: String = id.toString()
    
    /**
     * Configures the name of the config file in this addon's namespace.
     *
     * Example: `config("my_cfg")` -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun config(name: String) {
        this.configId = id.namespace() + ":" + name
    }
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     *
     * Examples:
     * * `rawConfig("some_addon:my_cfg")` -> `plugins/some_addon/configs/my_cfg.yml`
     * * `rawConfig("config") -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun rawConfig(id: String) {
        this.configId = id
    }
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     *
     * Examples:
     * * `rawConfig(Key.key("some_addon", "my_cfg"))` -> `plugins/some_addon/configs/my_cfg.yml`
     * * `rawConfig(Key.key("config")) -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun rawConfig(id: Key) {
        this.configId = id.toString()
    }
    
}