@file:OptIn(UnstableProviderApi::class)

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.plus
import xyz.xenondevs.commons.provider.provider
import io.papermc.paper.registry.tag.Tag as RegistryTagSet

//<editor-fold desc="emptyRegistryEntrySet">
/**
 * Returns an empty [RegistryEntrySet].
 */
fun <T : Keyed> emptyRegistryEntrySet(): RegistryEntrySet<T> = EmptyRegistryEntrySet

/**
 * Returns an empty [RegistryEntrySet.Nova.Direct] for the given [registry].
 */
fun <T : NovaRegistryElement<T>> emptyRegistryEntrySet(
    registry: NovaRegistry<T>
): RegistryEntrySet.Nova.Direct<T> = NovaDirectRegistryEntrySet(registry, emptySet())

/**
 * Returns an empty [RegistryEntrySet.Paper.Direct] for the given [registry].
 */
fun <T : Keyed> emptyRegistryEntrySet(
    registry: RegistryKey<T>
): RegistryEntrySet.Paper.Direct<T> = PaperDirectRegistryEntrySet(registry, emptySet())

//</editor-fold>

//<editor-fold desc="registryEntrySetOf Nova direct">
/**
 * Returns a [RegistryEntrySet.Nova] containing [elements].
 */
fun <T : NovaRegistryElement<T>> registryEntrySetOf(
    elements: Iterable<RegistryEntry.Nova<T>>,
): RegistryEntrySet.Nova.Direct<T> {
    val elementSet = elements.toSet()
    require(elementSet.isNotEmpty()) { "Elements cannot be empty" }
    
    val registry = elementSet.first().registry
    require(elementSet.all { it.registry == registry }) { "All entries must belong to the same registry" }
    
    return NovaDirectRegistryEntrySet(registry, elementSet)
}

/**
 * Returns a [RegistryEntrySet.Nova] containing ([element], [elements]).
 */
fun <T : NovaRegistryElement<T>> registryEntrySetOf(
    element: RegistryEntry.Nova<T>,
    vararg elements: RegistryEntry.Nova<T>
): RegistryEntrySet.Nova.Direct<T> {
    val registry = element.registry
    require(elements.all { it.registry == registry }) { "All entries must belong to the same registry" }
    return NovaDirectRegistryEntrySet(registry, setOf(element, *elements))
}

/**
 * Returns a [RegistryEntrySet.Paper] containing [elements].
 */
fun <T : Keyed> registryEntrySetOf(
    elements: Iterable<RegistryEntry.Paper<T>>,
): RegistryEntrySet.Paper.Direct<T> {
    val elementSet = elements.toSet()
    require(elementSet.isNotEmpty()) { "Elements cannot be empty" }
    
    val registry = elementSet.first().registry
    require(elementSet.all { it.registry == registry }) { "All entries must belong to the same registry" }
    
    return PaperDirectRegistryEntrySet(registry, elementSet)
}
//</editor-fold>

//<editor-fold desc="registryEntrySetOf Paper direct">
/**
 * Returns a [RegistryEntrySet.Paper] containing ([element], [elements]).
 */
fun <T : Keyed> registryEntrySetOf(
    element: RegistryEntry.Paper<T>,
    vararg elements: RegistryEntry.Paper<T>
): RegistryEntrySet.Paper.Direct<T> {
    val registry = element.registry
    require(elements.all { it.registry == registry }) { "All entries must belong to the same registry" }
    return PaperDirectRegistryEntrySet(registry, setOf(element, *elements))
}

/**
 * Returns a [RegistryEntrySet.Paper] from the given ([key], [keys]), resolving from [registryAccess].
 *
 * * If this function is called during bootstrap and a key is not present in the registry,
 *   an erroneous entry is created that throws [NoSuchElementException] when trying to resolve it.
 *   Additionally, server startup will fail.
 * * If this function is called after bootstrap and a key is not present in the registry,
 *   a [NoSuchElementException] is thrown immediately.
 */
fun <T : Keyed> registryEntrySetOf(
    key: TypedKey<T>,
    vararg keys: TypedKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): RegistryEntrySet.Paper.Direct<T> = registryEntrySetOf(listOf(key, *keys), registryAccess)

/**
 * Returns a [RegistryEntrySet.Paper] for the given [keys], resolving from [registryAccess].
 *
 * * If this function is called during bootstrap and a key is not present in the registry,
 *   an erroneous entry is created that throws [NoSuchElementException] when trying to resolve it.
 *   Additionally, server startup will fail.
 * * If this function is called after bootstrap and a key is not present in the registry,
 *   a [NoSuchElementException] is thrown immediately.
 */
fun <T : Keyed> registryEntrySetOf(
    keys: Iterable<TypedKey<T>>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): RegistryEntrySet.Paper.Direct<T> {
    val keyList = keys.toList()
    require(keyList.isNotEmpty()) { "Keys cannot be empty" }
    
    val registryKey = keyList[0].registryKey()
    require(keyList.all { it.registryKey() == registryKey }) { "All keys must belong to the same registry" }
    
    return PaperDirectRegistryEntrySet(
        registryKey,
        keyList.mapToSet { RegistryEntry.paper(it, registryAccess) }
    )
}
//</editor-fold>

//<editor-fold desc="registryEntrySetOf Paper tag">
/**
 * Returns a [RegistryEntrySet.Paper.Tag] for the given [tagKey], resolving from [registryAccess].
 *
 * * If this function is called during bootstrap and the tag doesn't exist in the registry,
 *   an erroneous [RegistryEntrySet.Paper.Tag] is returned that throws [NoSuchElementException] when trying to resolve it.
 *   Additionally, server startup will fail.
 * * If this function is called after bootstrap and the tag doesn't exist in the registry,
 *   a [NoSuchElementException] is thrown immediately.
 */
fun <T : Keyed> registryEntrySetOf(
    tagKey: TagKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): RegistryEntrySet.Paper.Tag<T> = PaperTagRegistryEntrySet(
    tagKey,
    PaperTagManager.getTagEntries(tagKey, registryAccess)
)

/**
 * Returns a provider of an [RegistryEntrySet.Paper.Tag] for the given [tagKey], resolving from [registryAccess],
 * or null if the tag doesn't exist in the registry.
 */
fun <T : Keyed> optionalRegistryEntrySetOf(
    tagKey: TagKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): Provider<RegistryEntrySet.Paper.Tag<T>?> =
    PaperTagManager.getOptionalTagEntries(tagKey, registryAccess).mapNonNull { PaperTagRegistryEntrySet(tagKey, it) }

/**
 * Returns a [RegistryEntrySet.Paper.Tag] for all entries in the given [registryKey], resolving from [registryAccess].
 * This uses a fake tag key using the registry key, which is not actually present in the registry.
 */
fun <T : Keyed> registryEntrySetOf(
    registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): RegistryEntrySet.Paper.Tag<T> = PaperTagRegistryEntrySet(
    TagKey.create(registryKey, registryKey.key()),
    PaperTagManager.getAllEntries(registryKey, registryAccess)
)
//</editor-fold>

/**
 * A set of [RegistryEntries][RegistryEntry].
 * [RegistryEntrySets][RegistryEntrySet] can be either:
 * - direct references to individual [RegistryEntries][RegistryEntry] ([RegistryEntrySet.Paper] / [RegistryEntrySet.Nova])
 * - a tag ([RegistryEntrySet.Paper.Tag] / [RegistryEntrySet.Nova.Tag]), which in turn can be composed of other tags and/or direct entries
 * 
 * Two [RegistryEntrySets][RegistryEntrySet] are considered equal (`==`) if they are of the same type (e.g. `Nova.Direct`, `Paper.Tag`),
 * reference the same registries, and have the same content. Direct and tag sets are never equal, even if they resolve to the same entries.
 * 
 * @see emptyRegistryEntrySet
 * @see registryEntrySetOf
 */
sealed interface RegistryEntrySet<out T : Keyed> : Provider<Set<T>> {
    
    /**
     * Checks whether [value] is part of this set.
     * Requires resolving this set and as such this function may not be called before registry freeze.
     */
    operator fun contains(value: @UnsafeVariance T): Boolean =
        value in get()
    
    /**
     * A [RegistryEntrySet] backed by a Paper registry.
     */
    sealed interface Paper<out T : Keyed> : RegistryEntrySet<T> {
        
        /**
         * The key of the registry this [RegistryEntrySet.Paper] belongs to.
         */
        val registry: RegistryKey<@UnsafeVariance T>
        
        /**
         * Converts this entry set to a [RegistrySet].
         * This may involve accessing the underlying registry via [registryAccess].
         * As such, this function may not be called before registry freeze.
         */
        fun toRegistryKeySet(
            registryAccess: RegistryAccess = RegistryAccess.registryAccess()
        ): RegistryKeySet<@UnsafeVariance T>
        
        /**
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Paper<@UnsafeVariance T>?): Boolean
        
        /**
         * A [RegistryEntrySet.Paper] backed by a constant set of entries.
         */
        sealed interface Direct<out T : Keyed> : Paper<T> {
            
            /**
             * The constant set of entries contained in this [RegistryEntrySet.Paper.Direct].
             */
            val entries: Set<RegistryEntry.Paper<T>>
            
            override fun toRegistryKeySet(registryAccess: RegistryAccess): RegistryKeySet<@UnsafeVariance T>
            
            /**
             * Checks whether [entry] is a part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Paper<@UnsafeVariance T>?): Boolean
            
        }
        
        /**
         * A [RegistryEntrySet.Paper] backed by a tag.
         */
        sealed interface Tag<out T : Keyed> : Paper<T> {
            
            /**
             * The key of the tag this [RegistryEntrySet.Paper.Tag] is backed by.
             */
            val tagKey: TagKey<@UnsafeVariance T>
            
            /**
             * The entries contained in this [RegistryEntrySet.Paper.Tag].
             */
            val entries: Provider<Set<RegistryEntry.Paper<T>>>
            
            override fun toRegistryKeySet(registryAccess: RegistryAccess): RegistryTagSet<@UnsafeVariance T>
            
            /**
             * Checks whether [entry] is a part of this set.
             * Requires resolving the corresponding tag and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override fun contains(entry: RegistryEntry.Paper<@UnsafeVariance T>?): Boolean
            
        }
        
    }
    
    /**
     * A [RegistryEntrySet] backed by a Nova registry.
     */
    sealed interface Nova<out T : NovaRegistryElement<T>> : RegistryEntrySet<T> {
        
        /**
         * The registry this [RegistryEntrySet.Nova] belongs to.
         */
        val registry: NovaRegistry<T>
        
        /**
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance T>?): Boolean
        
        /**
         * A [RegistryEntrySet.Nova] backed by a constant set of entries.
         */
        sealed interface Direct<out T : NovaRegistryElement<T>> : Nova<T> {
            
            /**
             * The constant set of entries contained in this [RegistryEntrySet.Nova.Direct].
             */
            val entries: Set<RegistryEntry.Nova<T>>
            
            /**
             * Checks whether [entry] is part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance T>?): Boolean
            
        }
        
        /**
         * A [RegistryEntrySet.Nova] backed by a tag.
         */
        sealed interface Tag<out T : NovaRegistryElement<T>> : Nova<T> {
            
            /**
             * The key of the tag this [RegistryEntrySet.Nova.Tag] is backed by.
             */
            val tagKey: Key
            
            /**
             * The entries contained in this [RegistryEntrySet.Nova.Tag].
             */
            val entries: Provider<Set<RegistryEntry.Nova<T>>>
            
            /**
             * Checks whether [entry] is part of this set.
             * Requires resolving the corresponding tag and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance T>?): Boolean
            
        }
        
    }
    
    
}

internal object EmptyRegistryEntrySet : RegistryEntrySet<Nothing>, Provider<Set<Nothing>> by provider(emptySet()) {
    override fun equals(other: Any?) = other === this || other is Provider<*> && other.delegate == delegate
    override fun hashCode() = System.identityHashCode(delegate)
    override fun toString() = "[]"
}

private class PaperDirectRegistryEntrySet<T : Keyed>(
    override val registry: RegistryKey<T>,
    override val entries: Set<RegistryEntry.Paper<T>>,
    values: Provider<Set<T>> = combinedProvider(entries.toList(), List<T>::toSet)
) : RegistryEntrySet.Paper.Direct<T>, Provider<Set<T>> by values {
    
    override fun toRegistryKeySet(registryAccess: RegistryAccess): RegistryKeySet<T> =
        RegistrySet.keySet(registry, entries.map { TypedKey.create(registry, it.key) })
    
    override fun contains(entry: RegistryEntry.Paper<T>?): Boolean =
        entry != null && entry in entries
    
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Paper.Direct<*>
                && other.registry == registry
                && other.entries == entries)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + entries.hashCode()
        return result
    }
    
    override fun toString() = "${registry.key().asString()}/[${entries.joinToString { it.key.asString() }}]"
    
}

private class PaperTagRegistryEntrySet<T : Keyed>(
    override val tagKey: TagKey<T>,
    override val entries: Provider<Set<RegistryEntry.Paper<T>>>,
    values: Provider<Set<T>> = entries.flatMap { combinedProvider(it.toList(), List<T>::toSet) }
) : RegistryEntrySet.Paper.Tag<T>, Provider<Set<T>> by values {
    
    override val registry: RegistryKey<T>
        get() = tagKey.registryKey()
    
    override fun toRegistryKeySet(registryAccess: RegistryAccess): RegistryTagSet<T> =
        registryAccess.getRegistry(registry).getTag(tagKey)
    
    override fun contains(entry: RegistryEntry.Paper<T>?): Boolean =
        entry != null && entry in entries.get()
    
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Paper.Tag<*>
                && other.registry == registry
                && other.tagKey == tagKey)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + tagKey.hashCode()
        return result
    }
    
    override fun toString() = "${registry.key().asString()}/#${tagKey.key().asString()}"
    
}

private class NovaDirectRegistryEntrySet<T : NovaRegistryElement<T>>(
    override val registry: NovaRegistry<T>,
    override val entries: Set<RegistryEntry.Nova<T>>,
    values: Provider<Set<T>> = combinedProvider(entries.toList(), List<T>::toSet)
) : RegistryEntrySet.Nova.Direct<T>, Provider<Set<T>> by values {
    
    
    override fun contains(entry: RegistryEntry.Nova<T>?): Boolean =
        entry != null && entry in entries
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Nova.Direct<*>
                && other.registry == registry
                && other.entries == entries)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + entries.hashCode()
        return result
    }
    
    override fun toString() = "${registry.key.asString()}/[${entries.joinToString { it.key.asString() }}]"
    
}

internal class NovaTagRegistryEntrySet<T : NovaRegistryElement<T>>(
    override val registry: NovaRegistry<T>,
    override val tagKey: Key,
    override val entries: Provider<Set<RegistryEntry.Nova<T>>>,
    values: Provider<Set<T>> = entries.flatMap { combinedProvider(it.toList(), List<T>::toSet) }
) : RegistryEntrySet.Nova.Tag<T>, Provider<Set<T>> by values {
    
    override fun contains(entry: RegistryEntry.Nova<T>?): Boolean =
        entry != null && entry in entries.get()
    
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Nova.Tag<*>
                && other.registry == registry
                && other.tagKey == tagKey)
    }
    
    override fun hashCode(): Int {
        var result = registry.hashCode()
        result = 31 * result + tagKey.hashCode()
        return result
    }
    
    override fun toString() = "${registry.key.asString()}/#${tagKey.asString()}"
    
}
