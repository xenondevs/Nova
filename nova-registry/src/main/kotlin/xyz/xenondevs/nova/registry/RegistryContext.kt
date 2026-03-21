package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Keyed
import xyz.xenondevs.commons.provider.Provider
import java.util.*

/**
 * Service for working with paper registries.
 */
interface RegistryContext {
    
    /**
     * Whether the server is currently in the bootstrap phase,
     * during which paper registries cannot be accessed.
     */
    val isInBootstrapPhase: Boolean
    
    /**
     * Remembers an unresolved entry that needs to be [resolved][Provider.get] to complete server startup.
     * Erroneous (unbound) entries will prevent startup.
     */
    fun trackUnresolved(identifier: Keyed, entry: Provider<*>)
    
    /**
     * Registers a lister that is called after tags of paper registries were reloaded.
     */
    fun registerPostTagReloadListener(listener: () -> Unit)
    
    /**
     * Service provider for [RegistryContext].
     */
    companion object : RegistryContext by ServiceLoader.load(
        RegistryContext::class.java,
        RegistryContext::class.java.classLoader
    ).single()
    
}