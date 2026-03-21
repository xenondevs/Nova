package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key

/**
 * A builder for something that has a dedicated configuration file.
 */
@RegistryElementBuilderDsl
interface ConfigurableBuilder {
    
    /**
     * Configures the name of the config file in this addon's namespace.
     *
     * Example: `config("my_cfg")` -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun config(name: String)
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     *
     * Examples:
     * * `rawConfig("some_addon:my_cfg")` -> `plugins/some_addon/configs/my_cfg.yml`
     * * `rawConfig("my_cfg")` -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun rawConfig(id: String)
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     *
     * Examples:
     * * `rawConfig(Key.key("some_addon", "my_cfg"))` -> `plugins/some_addon/configs/my_cfg.yml`
     * * `rawConfig(Key.key("my_addon", "my_cfg"))` -> `plugins/my_addon/configs/my_cfg.yml`
     */
    fun rawConfig(id: Key) {
        rawConfig(id.toString())
    }
    
}