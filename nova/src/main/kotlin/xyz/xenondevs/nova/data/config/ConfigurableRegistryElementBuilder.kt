
package xyz.xenondevs.nova.data.config

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.registry.RegistryElementBuilder

abstract class ConfigurableRegistryElementBuilder<T : Any>(
    registry: WritableRegistry<in T>,
    id: ResourceLocation
) : RegistryElementBuilder<T>(registry, id) {
    
    protected var configId: String = id.toString()
    
    /**
     * Configures the name of the config file in this addon's namespace.
     * 
     * Example: `config("my_cfg")` -> `configs/my_addon/my_cfg.yml`
     */
    fun config(name: String) {
        this.configId = id.namespace + ":" + name
    }
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     * 
     * Examples: 
     * * `rawConfig("my_addon:my_cfg")` -> `configs/my_addon/my_cfg.yml`
     * * `rawConfig("config") -> `configs/config.yml`
     */
    fun rawConfig(id: String) {
        this.configId = id
    }
    
    /**
     * Configures the id of this config file in the format `namespace:name`.
     * 
     * Examples:
     * * `rawConfig(ResourceLocation("my_addon:my_cfg"))` -> `configs/my_addon/my_cfg.yml`
     * * `rawConfig(ResourceLocation("config")) -> `configs/config.yml`
     */
    fun rawConfig(id: ResourceLocation) {
        this.configId = id.toString()
    }
    
}