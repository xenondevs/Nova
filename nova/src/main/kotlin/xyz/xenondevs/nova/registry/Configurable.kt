package xyz.xenondevs.nova.registry

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.ConfigProvider

val Provider<Configurable>.config: Provider<ConfigProvider>
    get() = map { it.config }

/**
 * Something that may have a config.
 */
interface Configurable {
    
    /**
     * The configuration for this [Configurable].
     * May be an empty node if the config file does not exist.
     * 
     * Reloaded when configs are reloaded.
     */
    val config: ConfigProvider
    
}