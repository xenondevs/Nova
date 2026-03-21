package xyz.xenondevs.nova.registry

import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.provider.Provider

/**
 * The configuration of the [Configurable] contained in this [Provider].
 * 
 * Equivalent to `entry.flatMap { it.config }`.
 */
val Provider<Configurable>.config: Provider<CommentedConfigurationNode>
    get() = flatMap(Configurable::config)

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
    val config: Provider<CommentedConfigurationNode>
    
}