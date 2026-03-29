package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
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
     * Remembers that an unresolved registry entry for [key] was created during bootstrap which
     * needs to be present to complete server startup.
     */
    fun <T : Keyed> trackUnresolvedEntry(
        key: TypedKey<T>,
        registryAccess: RegistryAccess
    )
    
    /**
     * Remembers that an unresolved either registry entry or tag for [key] was created during bootstrap which
     * needs to be present to complete server startup.
     */
    fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedEntry(
        key: TypedKey<T>,
        novaRegistry: NovaRegistry<N>,
        registryAccess: RegistryAccess
    )
    
    /**
     * Remembers that an unresolved registry entry set for [key] was created during bootstrap which
     * needs to be present to complete server startup.
     */
    fun <T : Keyed> trackUnresolvedTag(
        key: TagKey<T>,
        registryAccess: RegistryAccess
    )
    
    /**
     * Remembers that an unresolved mixed registry entry set for [key] was created during bootstrap which
     * needs to be present to complete server startup.
     */
    fun <N : NovaRegistryElement<N>, T : Keyed> trackUnresolvedTag(
        key: TagKey<T>,
        novaRegistry: NovaRegistry<N>,
        registryAccess: RegistryAccess
    )
    
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