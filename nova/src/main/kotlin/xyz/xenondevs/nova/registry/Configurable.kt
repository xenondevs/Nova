package xyz.xenondevs.nova.registry

import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.ksp.annotation.GenerateFlatMapExtensions

/**
 * Something that may have a config.
 */
@GenerateFlatMapExtensions
interface Configurable {
    
    /**
     * The configuration for this [Configurable].
     * May be an empty node if the config file does not exist.
     * 
     * Reloaded when configs are reloaded.
     */
    val config: Provider<CommentedConfigurationNode>
    
}