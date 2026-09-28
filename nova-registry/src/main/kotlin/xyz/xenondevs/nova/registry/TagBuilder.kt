@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import org.bukkit.Keyed
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider

/**
 * A builder for creating or modifying the entries of a tag in a pre-flattened state.
 */
sealed interface TagBuilder<T : Keyed, E : RegistryEntry<T>, S : RegistryEntrySet<T>> {
    
    /**
     * Adds the given direct [entries] to the tag.
     */
    fun add(entry: E, vararg entries: E) =
        add(provider(setOf(entry, *entries)))
    
    /**
     * Adds the direct entries supplied by [entry] and [entries] to the tag.
     */
    @JvmName("addEntryProviders")
    fun add(entry: Provider<E>, vararg entries: Provider<E>) =
        add(combinedProvider(listOf(entry, *entries)) { it })
    
    /**
     * Adds the given direct [entries] to the tag.
     */
    fun add(entries: Iterable<E>) =
        add(provider(entries.toSet()))
    
    /**
     * Adds the direct entries supplied by [entries] to the tag.
     */
    @JvmName("addEntriesProvider")
    fun add(entries: Provider<Iterable<E>>)
    
    /**
     * Adds the given [tag] to this tag.
     */
    fun add(tag: S) =
        add(provider(tag))
    
    /**
     * Adds the tags supplied by [tag] and [tags] to this tag.
     */
    @JvmName("addTagProvider")
    fun add(tag: Provider<S>)
    
    /**
     * Removes the given direct [entries] from the tag.
     * 
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    fun remove(entry: E, vararg entries: E) =
        remove(provider(setOf(entry, *entries)))
    
    /**
     * Removes the direct entries supplied by [entry] and [entries] from the tag.
     *
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    @JvmName("removeEntryProviders")
    fun remove(entry: Provider<E>, vararg entries: Provider<E>) =
        remove(combinedProvider(listOf(entry, *entries)) { it })
    
    /**
     * Removes the given direct [entries] from the tag.
     * 
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    fun remove(entries: Iterable<E>) =
        remove(provider(entries.toSet()))
    
    /**
     * Removes the direct entries supplied by [entries] from the tag.
     *
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    @JvmName("removeEntriesProvider")
    fun remove(entries: Provider<Iterable<E>>)
    
    /**
     * Removes the given [tag] from this tag.
     * 
     * Note that this does not remove entries that are directly included in this tag,
     * so it is not guaranteed that the resulting tag will not contain the entries in the given tag.
     */
    fun remove(tag: S) =
        remove(provider(tag))
    
    /**
     * Removes the tags supplied by [tag] and [tags] from this tag.
     *
     * Note that this does not remove entries that are directly included in this tag,
     * so it is not guaranteed that the resulting tag will not contain the entries in the given tags.
     */
    @JvmName("removeTagProvider")
    fun remove(tag: Provider<S>)
    
    /**
     * [TagBuilder] specific to Nova registries.
     */
    sealed interface Nova<T : NovaRegistryElement<T>> : TagBuilder<T, RegistryEntry.Nova<T>, RegistryEntrySet.Nova.Tag<T>>
    
    /**
     * [TagBuilder] specific to Paper registries.
     */
    sealed interface Paper<T : Keyed> : TagBuilder<T, RegistryEntry.Paper<T>, RegistryEntrySet.Paper.Tag<T>>
    
}