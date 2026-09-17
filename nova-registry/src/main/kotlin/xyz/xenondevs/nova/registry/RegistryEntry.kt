@file:OptIn(UnstableProviderApi::class)

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.provider

/**
 * Represents a key-value pair in a registry.
 * 
 * Two registry entries are considered equal `==` iff their registries and keys match.
 */
sealed interface RegistryEntry<out T : Keyed> : Provider<T> {
    
    /**
     * The key of the registry entry.
     */
    val key: Key
    
    /**
     * Represents a key-value pair in a Paper registry.
     */
    sealed interface Paper<out T : Keyed> : RegistryEntry<T> {
        
        /**
         * The registry key of the registry entry.
         */
        val registry: RegistryKey<@UnsafeVariance T>
        
    }
    
    /**
     * Represents a key-value pair in a Nova registry.
     */
    sealed interface Nova<out T : NovaRegistryElement<T>> : RegistryEntry<T> {
        
        /**
         * The registry of the registry entry.
         */
        val registry: NovaRegistry<T>
        
    }
    
    companion object {
        
        /**
         * Returns a [RegistryEntry.Paper] for the given [key], lazily resolving from [registryAccess].
         * 
         * * If this function is called during bootstrap and the key is not present in the registry,
         *   an erroneous [Paper] is returned that throws [NoSuchElementException] when trying to resolve it.
         *   Additionally, server startup will fail.
         * * If this function is called after bootstrap and the key is not present in the registry,
         *   a [NoSuchElementException] is thrown immediately.
         */
        fun <T : Keyed> paper(
            key: TypedKey<T>,
            registryAccess: RegistryAccess = RegistryAccess.registryAccess()
        ): Paper<T> {
            fun resolve(): T {
                return registryAccess.getRegistry(key.registryKey()).get(key)
                    ?: throw NoSuchElementException("No element under ${key.key().asString()} in registry ${key.registryKey().key().asString()}")
            }
            
            if (RegistryContext.isInBootstrapPhase) {
                val entry = PaperRegistryEntry(key, provider(::resolve))
                RegistryContext.trackUnresolvedEntry(key, registryAccess)
                return entry
            } else {
                return PaperRegistryEntry(key, provider(resolve()))
            }
        }
        
        /**
         * Returns a [RegistryEntry.Paper] for the given [value].
         * It is the callers responsibility to verify that the value is actually present in the registry.
         */
        fun <T : Keyed> paper(
            registry: RegistryKey<T>,
            value: T
        ): Paper<T> = PaperRegistryEntry(TypedKey.create(registry, value.key), provider(value))
        
        /**
         * Returns a provider of a [RegistryEntry.Paper] for the given [key], resolving from [registryAccess].
         * When the key is not present in the registry, the returned provider's value will be `null`.
         * Trying to resolve the returned provider before paper registries are available will result in an exception.
         */
        fun <T : Keyed> optionalPaper(
            key: TypedKey<T>,
            registryAccess: RegistryAccess = RegistryAccess.registryAccess()
        ): Provider<Paper<T>?> = provider {
            val value = registryAccess.getRegistry(key.registryKey()).get(key)
            if (value != null) PaperRegistryEntry(key, provider(value)) else null
        }
        
    }
    
}

/**
 * Flat-maps the value of this provider via [transform].
 * During [bootstrap phase][RegistryContext.isInBootstrapPhase], uses [Provider.flatMap], otherwise [Provider.immediateFlatMap].
 * This allows creating flat-mapped providers based off of [RegistryEntries][RegistryEntry] lazily during bootstrap phase,
 * where resolving their value is not possible, without paying for the extra overhead post-bootstrap, where [immediateFlatMap] can be used.
 * 
 * The idea behind this is that for non-reloadable registries, [immediateFlatMap] basically just calls `transform(get())`, so no intemediate
 * flat-mapping provider needs to be created. For reloadable registries, the difference behind [flatMap] and [immediateFlatMap] should be negligible.
 */
fun <T, R> Provider<T>.bootstrapFlatMap(transform: (T) -> Provider<R>): Provider<R> =
    if (RegistryContext.isInBootstrapPhase) flatMap(transform) else immediateFlatMap(transform)

/**
 * Flattens the value of this provider.
 * During [bootstrap phase][RegistryContext.isInBootstrapPhase], uses [Provider.flatMap], otherwise [Provider.immediateFlatMap].
 * This allows creating flat-mapped providers based off of [RegistryEntries][RegistryEntry] lazily during bootstrap phase,
 * where resolving their value is not possible, without paying for the extra overhead post-bootstrap, where [immediateFlatMap] can be used.
 * 
 * The idea behind this is that for non-reloadable registries, [immediateFlatMap] basically just calls `transform(get())`, so no intemediate
 * flat-mapping provider needs to be created. For reloadable registries, the difference behind [flatMap] and [immediateFlatMap] should be negligible.
 */
fun <T> Provider<Provider<T>>.bootstrapFlatten(): Provider<T> = bootstrapFlatMap { it }

private class PaperRegistryEntry<T : Keyed>(
    typedKey: TypedKey<T>,
    override val delegate: Provider<T>
) : RegistryEntry.Paper<T>, Provider<T> by delegate {
    
    override val key: Key = typedKey.key()
    override val registry: RegistryKey<T> = typedKey.registryKey()
    
    override fun equals(other: Any?): Boolean {
        return other === this ||
            (other is RegistryEntry.Paper<*>
                && other.registry == registry
                && other.key == key)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
    
    override fun toString(): String = "${registry.key().asString()}/${key.asString()}"
    
}

internal class NovaRegistryEntry<T : NovaRegistryElement<T>>(
    override val registry: NovaRegistry<T>,
    override val key: Key,
    override val delegate: Provider<T>
) : RegistryEntry.Nova<T>, Provider<T> by delegate {
    
    override fun equals(other: Any?): Boolean {
        return other === this ||
            (other is RegistryEntry.Nova<*>
                && other.registry == registry
                && other.key == key)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
    
    override fun toString(): String = "${registry.key.asString()}/${key.asString()}"
    
}
