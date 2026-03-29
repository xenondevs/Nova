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

/**
 * Returns an empty [RegistryEntrySet.Mixed.Direct] for the given [novaRegistry] and [paperRegistry].
 */
fun <N : NovaRegistryElement<N>, P : Keyed> emptyRegistryEntrySet(
    novaRegistry: NovaRegistry<N>,
    paperRegistry: RegistryKey<P>
): RegistryEntrySet.Mixed.Direct<N, P> = registryEntrySetOf(emptyRegistryEntrySet(novaRegistry), emptyRegistryEntrySet(paperRegistry))
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
    
    val registry = elementSet.first().key.registryKey()
    require(elementSet.all { it.key.registryKey() == registry }) { "All entries must belong to the same registry" }
    
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
    val registry = element.key.registryKey()
    require(elements.all { it.key.registryKey() == registry }) { "All entries must belong to the same registry" }
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

//<editor-fold desc="registryEntrySetOf mixed direct">
/**
 * Returns a [RegistryEntrySet.Mixed.Direct] that contains all entries of both the [nova] and [paper] sets.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    nova: RegistryEntrySet.Nova.Direct<N>,
    paper: RegistryEntrySet.Paper.Direct<P>
): RegistryEntrySet.Mixed.Direct<N, P> = MixedDirectRegistryEntrySet(
    nova.registry,
    paper.registry,
    combineToEither(nova.entries, paper.entries, nova.registry, paper.registry),
    nova + paper
)

/**
 * Returns a [RegistryEntrySet.Mixed.Direct] of [novaRegistry] and [paperRegistry] that contains all [entries].
 * 
 * @throws IllegalArgumentException If any entry is from registries other than [novaRegistry] and [paperRegistry].
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    entries: Iterable<RegistryEntry.Either<N, P>>,
    novaRegistry: NovaRegistry<N>,
    paperRegistry: RegistryKey<P>
): RegistryEntrySet.Mixed.Direct<N, P> = MixedDirectRegistryEntrySet(
    novaRegistry,
    paperRegistry,
    entries.toSet(),
    combinedProvider(entries.toList()).map { it.toSet() }
)

/**
 * Returns a [RegistryEntrySet.Mixed.Direct] that contains all of ([element], [elements]).
 * 
 * @throws IllegalArgumentException If not all entries are from the same Nova and Paper registries.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    element: RegistryEntry.Either<N, P>,
    vararg elements: RegistryEntry.Either<N, P>
): RegistryEntrySet.Mixed.Direct<N, P> = registryEntrySetOf(listOf(element, *elements))

/**
 * Returns a [RegistryEntrySet.Mixed.Direct] that contains all [elements].
 * 
 * @throws IllegalArgumentException If [elements] is empty.
 * @throws IllegalArgumentException If not all entries in [elements] are from the same Nova and Paper registries.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    elements: Iterable<RegistryEntry.Either<N, P>>
): RegistryEntrySet.Mixed.Direct<N, P> {
    val elements = elements.toSet()
    require(elements.isNotEmpty()) { "Elements cannot be empty" }
    val element: RegistryEntry.Either<N, P> = elements.first()
    require(elements.all {
        it.novaRegistry == element.novaRegistry
            && it.paperRegistry == element.paperRegistry
    }) { "All entries must belong to the same registries" }
    
    return MixedDirectRegistryEntrySet(
        element.novaRegistry,
        element.paperRegistry,
        elements,
        combinedProvider(elements.toList()) { it.toSet() }
    )
}

/**
 * Returns a [RegistryEntrySet.Mixed.Direct] by wrapping this [RegistryEntrySet.Nova.Direct] with an empty [paperRegistry] part.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> RegistryEntrySet.Nova.Direct<N>.asMixed(
    paperRegistry: RegistryKey<P>
): RegistryEntrySet.Mixed.Direct<N, P> = registryEntrySetOf(this, emptyRegistryEntrySet(paperRegistry))

/**
 * Returns a [RegistryEntrySet.Mixed.Direct] by wrapping this [RegistryEntrySet.Paper.Direct] with an empty [novaRegistry] part.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> RegistryEntrySet.Paper.Direct<P>.asMixed(
    novaRegistry: NovaRegistry<N>,
): RegistryEntrySet.Mixed.Direct<N, P> = registryEntrySetOf(emptyRegistryEntrySet(novaRegistry), this)
//</editor-fold>

//<editor-fold desc="registryEntrySetOf mixed tag">
/**
 * Returns a [RegistryEntrySet.Mixed.Tag] that contains all entries of both the [nova] and [paper] sets.
 * 
 * @throws IllegalArgumentException If the sets don't use the same tag key.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    nova: RegistryEntrySet.Nova.Tag<N>,
    paper: RegistryEntrySet.Paper.Tag<P>
): RegistryEntrySet.Mixed.Tag<N, P> {
    require(nova.tagKey == paper.tagKey) { "Tag keys must be the same for both Nova and Paper entry sets" }
    return MixedTagRegistryEntrySet(nova.tagKey, nova, paper)
}

/**
 * Returns a [RegistryEntrySet.Mixed.Tag] for [tagKey], resolved and merged together from both
 * [novaRegistry] and [paperRegistry] (using [registryAccess]).
 *
 * * If this function is called during bootstrap and no matching tag is found in either registry,
 *   an empty [RegistryEntrySet.Mixed.Tag] is returned.
 *   Additionally, server startup will fail.
 * * If this function is called after bootstrap and no matching tag is found in either registry,
 *   a [NoSuchElementException] is thrown immediately.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> registryEntrySetOf(
    tagKey: Key,
    novaRegistry: NovaRegistry<N>,
    paperRegistry: RegistryKey<P>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): RegistryEntrySet.Mixed.Tag<N, P> {
    val paperTagKey = TagKey.create(paperRegistry, tagKey)
    if (RegistryContext.isInBootstrapPhase) {
        val entrySet = MixedTagRegistryEntrySet(tagKey, novaRegistry, paperRegistry, registryAccess)
        RegistryContext.trackUnresolvedTag(paperTagKey, novaRegistry, registryAccess)
        return entrySet
    } else {
        val hasNovaTag = novaRegistry.getOptionalTag(tagKey).get() != null
        val hasPaperTag = registryAccess.getRegistry(paperRegistry).hasTag(paperTagKey)
        
        if (!hasNovaTag && !hasPaperTag)
            throw NoSuchElementException("Tag $tagKey not found in either ${novaRegistry.key.asString()} or ${paperRegistry.key().asString()}")
        
        return MixedTagRegistryEntrySet(tagKey, novaRegistry, paperRegistry, registryAccess)
    }
}

/**
 * Returns a [RegistryEntrySet.Mixed.Tag] by wrapping this [RegistryEntrySet.Nova.Tag] with an empty [paperRegistry] part.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> RegistryEntrySet.Nova.Tag<N>.asMixed(
    paperRegistry: RegistryKey<P>
): RegistryEntrySet.Mixed.Tag<N, P> = MixedTagRegistryEntrySet(
    tagKey.key(),
    this,
    emptyRegistryEntrySet(paperRegistry)
)

/**
 * Returns a [RegistryEntrySet.Mixed.Tag] by wrapping this [RegistryEntrySet.Paper.Tag] with an empty [novaRegistry] part.
 */
fun <N : NovaRegistryElement<N>, P : Keyed> RegistryEntrySet.Paper.Tag<P>.asMixed(
    novaRegistry: NovaRegistry<N>,
): RegistryEntrySet.Mixed.Tag<N, P> = MixedTagRegistryEntrySet(
    tagKey.key(),
    emptyRegistryEntrySet(novaRegistry),
    this
)
//</editor-fold>

//<editor-fold desc="reactive transformations">
/**
 * Maps each element of this [RegistryEntrySet.Mixed] using [transformNova] for Nova entries
 * and [transformPaper] for Paper entries.
 */
inline fun <reified N : NovaRegistryElement<N>, reified P : Keyed, R> RegistryEntrySet.Mixed<N, P>.mapEach(
    crossinline transformNova: (N) -> R,
    crossinline transformPaper: (P) -> R
): Provider<List<R>> = mapEach {
    when (it) {
        is N -> transformNova(it)
        is P -> transformPaper(it)
        else -> throw AssertionError("Value $value is neither ${N::class.java} nor ${P::class.java}")
    }
}

/**
 * Maps each element of this [RegistryEntrySet.Mixed] using [transformNova] for Nova entries
 * and [transformPaper] for Paper entries, then flattens the resulting [Providers][Provider] into a single [Provider].
 */
inline fun <reified N : NovaRegistryElement<N>, reified P : Keyed, R> RegistryEntrySet.Mixed<N, P>.flatMapEach(
    crossinline transformNova: (N) -> Provider<R>,
    crossinline transformPaper: (P) -> Provider<R>
): Provider<List<R>> = mapEach(transformNova, transformPaper).map(::combinedProvider).flatten()
//</editor-fold>

/**
 * A set of [RegistryEntries][RegistryEntry].
 * [RegistryEntrySets][RegistryEntrySet] can be either:
 * - direct references to individual [RegistryEntries][RegistryEntry] ([RegistryEntrySet.Paper] / [RegistryEntrySet.Nova])
 * - a tag ([RegistryEntrySet.Paper.Tag] / [RegistryEntrySet.Nova.Tag]), which in turn can be composed of other tags and/or direct entries
 * - a [RegistryEntrySet.Mixed] which either combines two entry sets of corresponding Paper- and Nova registries (e.g. `NovaItem` and `ItemType`)
 * 
 * Two [RegistryEntrySets][RegistryEntrySet] are considered equal (`==`) if they are of the same type (e.g. `Nova.Direct`, `Mixed.Tag`),
 * reference the same registries, and have the same content. Direct and tag sets are never equal, even if they resolve to the same entries.
 * 
 * @see emptyRegistryEntrySet
 * @see registryEntrySetOf
 */
sealed interface RegistryEntrySet<out T : Keyed> : Provider<Set<T>> {
    
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
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set, 
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Either<*, @UnsafeVariance T>?): Boolean
        
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
            
            /**
             * Checks whether [entry] is a part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Either<*, @UnsafeVariance T>?): Boolean
            
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
            override fun contains(entry: RegistryEntry.Either<*, @UnsafeVariance T>?): Boolean
            
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
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance T, *>?): Boolean
        
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
            
            /**
             * Checks whether [entry] is part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance T, *>?): Boolean
            
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
            
            /**
             * Checks whether [entry] is part of this set.
             * Requires resolving the corresponding tag and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance T, *>?): Boolean
            
        }
        
    }
    
    /**
     * A [RegistryEntrySet] that contains entries which can be either from a Nova registry ([N]) or a Paper registry ([P]).
     * [N] and [P] should be corresponding concepts like `NovaItem` and `ItemType`.
     */
    sealed interface Mixed<out N : NovaRegistryElement<N>, out P : Keyed> : RegistryEntrySet<Keyed> {
        
        /**
         * The Nova registry some entries of this [RegistryEntrySet.Mixed] may belong to.
         */
        val novaRegistry: NovaRegistry<N>
        
        /**
         * A key to the Paper registry some entries of this [RegistryEntrySet.Mixed] may belong to.
         */
        val paperRegistry: RegistryKey<@UnsafeVariance P>
        
        /**
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance N, @UnsafeVariance P>?): Boolean
        
        /**
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance N>?): Boolean
        
        /**
         * Checks whether [entry] is part of this set.
         * Depending on whether this is a [Direct] or [Tag] set,
         * this may involve resolving this set and as such this function may not be called before registry freeze.
         */
        operator fun contains(entry: RegistryEntry.Paper<@UnsafeVariance P>?): Boolean
        
        /**
         * A [RegistryEntrySet.Mixed] backed by constant sets of entries in both the Nova and Paper part.
         */
        sealed interface Direct<N : NovaRegistryElement<N>, P : Keyed> : Mixed<N, P> {
            
            /**
             * The constant set of entries contained in this [RegistryEntrySet.Mixed.Direct],
             * combining the entries from both the Nova and Paper part.
             */
            val entries: Set<RegistryEntry.Either<N, P>>
            
            /**
             * Checks whether [entry] is part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance N, @UnsafeVariance P>?): Boolean
            
            /**
             * Checks whether [entry] is part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance N>?): Boolean
            
            /**
             * Checks whether [entry] is part of this set.
             * Does not resolve anything and is safe to call before registry freeze.
             */
            override operator fun contains(entry: RegistryEntry.Paper<@UnsafeVariance P>?): Boolean
            
        }
        
        /**
         * A [RegistryEntrySet.Mixed] backed by tags in both the Nova and Paper part, using the
         * same tag key in both parts.
         */
        sealed interface Tag<N : NovaRegistryElement<N>, P : Keyed> : Mixed<N, P> {
            
            /**
             * The key of the tag this [RegistryEntrySet.Mixed.Tag] is backed by.
             */
            val tagKey: Key
            
            /**
             * The entries contained in this [RegistryEntrySet.Mixed.Tag],
             * combining the entries from both the Nova and Paper part.
             */
            val entries: Provider<Set<RegistryEntry.Either<N, P>>>
            
            /**
             * Checks whether [entry] is part of this set.
             * Requires resolving the corresponding tags and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override operator fun contains(entry: RegistryEntry.Either<@UnsafeVariance N, @UnsafeVariance P>?): Boolean
            
            /**
             * Checks whether [entry] is part of this set.
             * Requires resolving the corresponding tags and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override operator fun contains(entry: RegistryEntry.Nova<@UnsafeVariance N>?): Boolean
            
            /**
             * Checks whether [entry] is part of this set.
             * Requires resolving the corresponding tags and cannot be called before registry freeze.
             * Also note that tag contents can change at any time.
             */
            override operator fun contains(entry: RegistryEntry.Paper<@UnsafeVariance P>?): Boolean
            
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
        RegistrySet.keySet(registry, entries.map { it.key })
    
    override fun contains(entry: RegistryEntry.Paper<T>?): Boolean =
        entry != null && entry in entries
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Either<*, T>?): Boolean =
        entry != null && entry in (entries as Set<RegistryEntry.Either<*, T>>)
    
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
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Either<*, T>?): Boolean =
        entry != null && entry in (entries.get() as Set<RegistryEntry.Either<*, T>>)
    
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
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Either<T, *>?): Boolean =
        entry != null && entry in (entries as Set<RegistryEntry.Either<T, *>>)
    
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
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Either<T, *>?): Boolean =
        entry != null && entry in (entries.get() as Set<RegistryEntry.Either<T, *>>)
    
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

private fun <N : NovaRegistryElement<N>, P : Keyed> combineToEither(
    novaEntries: Set<RegistryEntry.Nova<N>>,
    paperEntries: Set<RegistryEntry.Paper<P>>,
    novaRegistry: NovaRegistry<N>,
    paperRegistry: RegistryKey<P>
): Set<RegistryEntry.Either<N, P>> {
    val novaEithers = novaEntries.asSequence().map { RegistryEntry.either(it, paperRegistry) }
    val paperEithers = paperEntries.asSequence().map { RegistryEntry.either(novaRegistry, it) }
    return (novaEithers + paperEithers).toSet()
}

private val <T : NovaRegistryElement<T>> RegistryEntrySet.Nova<T>.entries: Provider<Set<RegistryEntry.Nova<T>>>
    get() = when (this) {
        is RegistryEntrySet.Nova.Direct -> provider(entries)
        is RegistryEntrySet.Nova.Tag -> entries
    }

private val <T : Keyed> RegistryEntrySet.Paper<T>.entries: Provider<Set<RegistryEntry.Paper<T>>>
    get() = when (this) {
        is RegistryEntrySet.Paper.Direct -> provider(entries)
        is RegistryEntrySet.Paper.Tag -> entries
    }

private fun <N : NovaRegistryElement<N>, P : Keyed> Iterable<RegistryEntry.Nova<N>>.toEither(
    paperRegistry: RegistryKey<P>
): Set<RegistryEntry.Either<N, P>> = asSequence().map { RegistryEntry.either(it, paperRegistry) }.toSet()

private fun <N : NovaRegistryElement<N>, P : Keyed> Iterable<RegistryEntry.Paper<P>>.toEither(
    novaRegistry: NovaRegistry<N>
): Set<RegistryEntry.Either<N, P>> = asSequence().map { RegistryEntry.either(novaRegistry, it) }.toSet()

private class MixedTagRegistryEntrySet<N : NovaRegistryElement<N>, P : Keyed>(
    override val novaRegistry: NovaRegistry<N>,
    override val paperRegistry: RegistryKey<P>,
    override val tagKey: Key,
    override val entries: Provider<Set<RegistryEntry.Either<N, P>>>,
    values: Provider<Set<Keyed>> = entries.flatMap { combinedProvider(it.toList(), List<Keyed>::toSet) }
) : RegistryEntrySet.Mixed.Tag<N, P>, Provider<Set<Keyed>> by values {
    
    constructor(tagKey: Key, nova: RegistryEntrySet.Nova<N>, paper: RegistryEntrySet.Paper<P>) : this(
        nova.registry,
        paper.registry,
        tagKey,
        combinedProvider(nova.entries, paper.entries) { n, p -> combineToEither(n, p, nova.registry, paper.registry) },
        nova + paper
    )
    
    constructor(tagKey: Key, novaRegistry: NovaRegistry<N>, paperRegistry: RegistryKey<P>, registryAccess: RegistryAccess) : this(
        novaRegistry,
        paperRegistry,
        tagKey,
        combinedProvider(
            novaRegistry.getOptionalTag(tagKey),
            optionalRegistryEntrySetOf(TagKey.create(paperRegistry, tagKey), registryAccess)
        ) { n, p ->
            combinedProvider(
                p?.entries?.map { it.toEither(novaRegistry) } ?: provider(emptySet()),
                n?.entries?.map { it.toEither(paperRegistry) } ?: provider(emptySet())
            ) { nEithers, pEithers -> nEithers + pEithers }
        }.flatten()
    )
    
    override fun contains(entry: RegistryEntry.Either<N, P>?): Boolean =
        entry != null && entry in entries.get()
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Nova<N>?): Boolean =
        entry != null && entry in (entries.get() as Set<RegistryEntry.Nova<N>>)
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Paper<P>?): Boolean =
        entry != null && entry in (entries.get() as Set<RegistryEntry.Paper<P>>)
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Mixed.Tag<*, *>
                && other.novaRegistry == novaRegistry
                && other.paperRegistry == paperRegistry
                && other.tagKey == tagKey)
    }
    
    override fun hashCode(): Int {
        var result = novaRegistry.hashCode()
        result = 31 * result + paperRegistry.hashCode()
        result = 31 * result + tagKey.hashCode()
        return result
    }
    
    override fun toString() = "${novaRegistry.key.asString()}|${paperRegistry.key().asString()}/#${tagKey.asString()}"
    
}

private class MixedDirectRegistryEntrySet<N : NovaRegistryElement<N>, P : Keyed>(
    override val novaRegistry: NovaRegistry<N>,
    override val paperRegistry: RegistryKey<P>,
    override val entries: Set<RegistryEntry.Either<N, P>>,
    values: Provider<Set<Keyed>>
) : RegistryEntrySet.Mixed.Direct<N, P>, Provider<Set<Keyed>> by values {
    
    init {
        require(entries.all { it.novaRegistry == novaRegistry && it.paperRegistry == paperRegistry }) { "All entries must belong to the specified registries" }
    }
    
    override fun contains(entry: RegistryEntry.Either<N, P>?): Boolean =
        entry != null && entry in entries
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Nova<N>?): Boolean =
        entry != null && entry in (entries as Set<RegistryEntry.Nova<N>>)
    
    @Suppress("UNCHECKED_CAST")
    override fun contains(entry: RegistryEntry.Paper<P>?): Boolean =
        entry != null && entry in (entries as Set<RegistryEntry.Paper<P>>)
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
            (other is RegistryEntrySet.Mixed.Direct<*, *>
                && other.novaRegistry == novaRegistry
                && other.paperRegistry == paperRegistry
                && other.entries == entries)
    }
    
    override fun hashCode(): Int {
        var result = novaRegistry.hashCode()
        result = 31 * result + paperRegistry.hashCode()
        result = 31 * result + entries.hashCode()
        return result
    }
    
    override fun toString() = "${novaRegistry.key.asString()}|${paperRegistry.key().asString()}/[${entries.joinToString { it.key.asString() }}]"
    
}