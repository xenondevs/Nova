package xyz.xenondevs.nova.registry

import org.bukkit.Keyed

/**
 * A builder for creating or modifying the entries of a tag in a pre-flattened state.
 */
sealed interface TagBuilder<T : Keyed, E : RegistryEntry<T>, S : RegistryEntrySet<T>> {
    
    /**
     * Adds the given direct [entries] to the tag.
     */
    fun add(entry: E, vararg entries: E) =
        add(setOf(entry, *entries))
    
    /**
     * Adds the given direct [entries] to the tag.
     */
    fun add(entries: Iterable<E>)
    
    /**
     * Adds the given [tag] to this tag.
     */
    fun add(tag: S)
    
    /**
     * Removes the given direct [entries] from the tag.
     * 
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    fun remove(entry: E, vararg entries: E) =
        remove(setOf(entry, *entries))
    
    /**
     * Removes the given direct [entries] from the tag.
     * 
     * Note that this does not remove entries that are included transitively through other tags,
     * so it is not guaranteed that the resulting tag will not contain the given entries.
     */
    fun remove(entries: Iterable<E>)
    
    /**
     * Removes the given [tag] from this tag.
     * 
     * Note that this does not remove entries that are directly included in this tag,
     * so it is not guaranteed that the resulting tag will not contain the entries in the given tag.
     */
    fun remove(tag: S)
    
    /**
     * [TagBuilder] specific to Nova registries.
     */
    sealed interface Nova<T : NovaRegistryElement<T>> : TagBuilder<T, RegistryEntry.Nova<T>, RegistryEntrySet.Nova.Tag<T>>
    
    /**
     * [TagBuilder] specific to Paper registries.
     */
    sealed interface Paper<T : Keyed> : TagBuilder<T, RegistryEntry.Paper<T>, RegistryEntrySet.Paper.Tag<T>>
    
}