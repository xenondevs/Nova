package xyz.xenondevs.nova.registry

import java.util.*

/**
 * Service for working with paper registries during the bootstrap phase.
 */
interface RegistryBootstrapContext {
    
    /**
     * Whether the server is currently in the bootstrap phase,
     * during which paper registries cannot be accessed.
     */
    val isInBootstrapPhase: Boolean
    
    /**
     * Remembers an unresolved entry that needs to be validated to complete server startup.
     * Erroneous (unbound) entries will prevent startup.
     */
    fun trackUnresolvedEntry(entry: RegistryEntry<*>)
    
    /**
     * Service provider for [RegistryBootstrapContext].
     */
    companion object : RegistryBootstrapContext by ServiceLoader.load(
        RegistryBootstrapContext::class.java,
        RegistryBootstrapContext::class.java.classLoader
    ).single()
    
}