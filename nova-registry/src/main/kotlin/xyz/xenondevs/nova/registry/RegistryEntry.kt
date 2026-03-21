@file:OptIn(UnstableProviderApi::class)

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider

/**
 * Represents a key-value pair in a registry.
 * 
 * Comparable, naturally ordered by registry key and then by entry key.
 * 
 * Two registry entries are considered equal `==` iff their registries and keys match.
 * A [RegistryEntry.Either] is equal to both the corresponding [RegistryEntry.Nova] and [RegistryEntry.Paper],
 * even though it only resolves to one of them.
 */
sealed interface RegistryEntry<out T : Keyed> : Provider<T>, Comparable<RegistryEntry<@UnsafeVariance T>> {
    
    /**
     * The key of the registry entry.
     */
    val key: Key
    
    /**
     * Represents a key-value pair in a Paper registry.
     */
    sealed interface Paper<out T : Keyed> : RegistryEntry<T> {
        
        /**
         * The typed key of the registry entry.
         */
        override val key: TypedKey<@UnsafeVariance T>
        
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
    
    /**
     * Represents a key-value pair that can be from either a Nova registry or a Paper registry.
     * [N] and [P] should be corresponding concepts like `NovaItem` and `ItemType`.
     * 
     * Comparable, naturally ordered by nova registry key and then by entry key.
     */
    sealed interface Either<out N : NovaRegistryElement<N>, out P : Keyed> : RegistryEntry<Keyed> {
        
        /**
         * The Nova registry that this entry may belong to.
         */
        val novaRegistry: NovaRegistry<N>
        
        /**
         * The key of the Paper registry that this entry may belong to.
         */
        val paperRegistry: RegistryKey<@UnsafeVariance P>
        
    }
    
    companion object {
        
        /**
         * Returns an [Either] for [key] that is either in [novaRegistry] or in [paperRegistry] from [registryAccess].
         * If the key is in both registries, the Nova registry will take precedence.
         * 
         * * If this function is called during bootstrap and the key exists in neither registry,
         *   an erroneous [Either] is returned that throws [NoSuchElementException] when trying to resolve it.
         *   Additionally, server startup will fail.
         * * If this function is called after bootstrap and the key exists in neither registry,
         *   a [NoSuchElementException] is thrown immediately.
         */
        fun <N : NovaRegistryElement<N>, P : Keyed> either(
            key: Key,
            novaRegistry: NovaRegistry<N>,
            paperRegistry: RegistryKey<P>,
            registryAccess: RegistryAccess = RegistryAccess.registryAccess()
        ): Either<N, P> {
            if (RegistryContext.isInBootstrapPhase) {
                val typedKey = TypedKey.create(paperRegistry, key)
                val entry = EitherRegistryEntry(
                    key,
                    novaRegistry,
                    paperRegistry,
                    combinedProvider(
                        novaRegistry.getOptional(key),
                        optionalPaper(typedKey, registryAccess)
                    ) { nova, paper -> nova ?: paper }.flatMap {
                        it ?: throw NoSuchElementException("Key $key not found in either ${novaRegistry.key.asString()} or ${paperRegistry.key().asString()}")
                    }
                )
                RegistryContext.trackUnresolved(typedKey, entry)
                return entry
            } else {
                if (key in novaRegistry) {
                    val novaEntry = novaRegistry[key]
                    return EitherRegistryEntry(key, novaRegistry, paperRegistry, novaEntry)
                } else {
                    val paperValue = registryAccess.getRegistry(paperRegistry).get(key)
                        ?: throw NoSuchElementException("No element under ${key.asString()} in registry ${novaRegistry.key.asString()} or ${paperRegistry.key().asString()}")
                    return EitherRegistryEntry(key, novaRegistry, paperRegistry, provider(paperValue))
                }
            }
        }
        
        /**
         * Returns an [Either] with the value [nova] and the unused [paperRegistry].
         */
        fun <N : NovaRegistryElement<N>, P : Keyed> either(nova: Nova<N>, paperRegistry: RegistryKey<P>): Either<N, P> =
            EitherRegistryEntry(nova.key, nova.registry, paperRegistry, nova.delegate)
        
        /**
         * Returns an [Either] with the value [paper] and the unused [novaRegistry].
         */
        fun <N : NovaRegistryElement<N>, P : Keyed> either(novaRegistry: NovaRegistry<N>, paper: Paper<P>): Either<N, P> =
            EitherRegistryEntry(paper.key, novaRegistry, paper.registry, paper.delegate)
        
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
                    ?: throw NoSuchElementException("No element under ${key.asString()} in registry ${key.registryKey().key().asString()}")
            }
            
            if (RegistryContext.isInBootstrapPhase) {
                val entry = PaperRegistryEntry(key, provider(::resolve))
                RegistryContext.trackUnresolved(key, entry)
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
 * [Maps][Provider.map] the value of this registry entry to a value of type [R] using either [transformNova] or [transformPaper],
 * depending on whether the entry is from a Nova registry or a Paper registry.
 */
inline fun <reified N : NovaRegistryElement<N>, reified P : Keyed, R> RegistryEntry.Either<N, P>.map(
    crossinline transformNova: (N) -> R,
    crossinline transformPaper: (P) -> R
): Provider<R> = map { value ->
    when (value) {
        is N -> transformNova(value)
        is P -> transformPaper(value)
        else -> throw AssertionError("Value $value is neither ${N::class.java} nor ${P::class.java}")
    }
}

/**
 * [Flat-maps][Provider.flatMap] the value of this registry entry to a value of type [R] using either [transformNova] or [transformPaper],
 * depending on whether the entry is from a Nova registry or a Paper registry.
 */
inline fun <reified N : NovaRegistryElement<N>, reified P : Keyed, R> RegistryEntry.Either<N, P>.flatMap(
    crossinline transformNova: (N) -> Provider<R>,
    crossinline transformPaper: (P) -> Provider<R>
): Provider<R> = flatMap { value ->
    when (value) {
        is N -> transformNova(value)
        is P -> transformPaper(value)
        else -> throw AssertionError("Value $value is neither ${N::class.java} nor ${P::class.java}")
    }
}

private fun comparisonRegistryKey(entry: RegistryEntry<*>): Key = when (entry) {
    is RegistryEntry.Paper -> entry.key.registryKey().key()
    is RegistryEntry.Nova -> entry.registry.key
    is RegistryEntry.Either<*, *> -> entry.novaRegistry.key
}

private class PaperRegistryEntry<T : Keyed>(
    override val key: TypedKey<T>,
    override val delegate: Provider<T>
) : RegistryEntry.Paper<T>, Provider<T> by delegate {
    
    override val registry: RegistryKey<T>
        get() = key.registryKey()
    
    override fun equals(other: Any?): Boolean {
        return other === this ||
            (other is RegistryEntry.Paper<*>
                && other.registry == registry
                && symmetricKeyEquals(other.key, key)) ||
            (other is RegistryEntry.Either<*, *>
                && other.paperRegistry == registry
                && symmetricKeyEquals(other.key, key))
    }
    
    override fun hashCode(): Int = symmetricKeyHashCode(key)
    
    override fun compareTo(other: RegistryEntry<T>): Int {
        val registryComparison = key.registryKey().key().compareTo(comparisonRegistryKey(other))
        if (registryComparison != 0)
            return registryComparison
        return key.compareTo(other.key)
    }
    
    override fun toString(): String = key.asString()
    
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
                && symmetricKeyEquals(other.key, key)) ||
            (other is RegistryEntry.Either<*, *>
                && other.novaRegistry == registry
                && symmetricKeyEquals(other.key, key))
    }
    
    override fun hashCode(): Int = symmetricKeyHashCode(key)
    
    override fun compareTo(other: RegistryEntry<T>): Int {
        val registryComparison = registry.key.compareTo(comparisonRegistryKey(other))
        if (registryComparison != 0)
            return registryComparison
        return key.compareTo(other.key)
    }
    
    override fun toString(): String = key.asString()
    
}

private class EitherRegistryEntry<N : NovaRegistryElement<N>, P : Keyed>(
    override val key: Key,
    override val novaRegistry: NovaRegistry<N>,
    override val paperRegistry: RegistryKey<P>,
    override val delegate: Provider<Keyed>
) : RegistryEntry.Either<N, P>, Provider<Keyed> by delegate {
    
    override fun equals(other: Any?): Boolean {
        return other === this ||
            (other is RegistryEntry.Either<*, *>
                && other.novaRegistry == novaRegistry
                && other.paperRegistry == paperRegistry
                && symmetricKeyEquals(other.key, key)) ||
            (other is RegistryEntry.Nova<*>
                && other.registry == novaRegistry
                && symmetricKeyEquals(other.key, key)) ||
            (other is RegistryEntry.Paper<*>
                && other.registry == paperRegistry
                && symmetricKeyEquals(other.key, key))
    }
    
    override fun hashCode(): Int = symmetricKeyHashCode(key)
    
    override fun compareTo(other: RegistryEntry<Keyed>): Int {
        val registryComparison = novaRegistry.key.compareTo(comparisonRegistryKey(other))
        if (registryComparison != 0)
            return registryComparison
        return key.compareTo(other.key)
    }
    
    override fun toString(): String = key.asString()
    
}

// https://github.com/PaperMC/Paper/issues/13678
private fun symmetricKeyEquals(a: Key, b: Key): Boolean {
    if (a is TypedKey<*> && b is TypedKey<*> && a.registryKey() != b.registryKey())
        return false
    return a.namespace() == b.namespace() && a.value() == b.value()
}

private fun symmetricKeyHashCode(key: Key): Int {
    var result = key.namespace().hashCode()
    result = 31 * result + key.value().hashCode()
    return result
}