package xyz.xenondevs.nova.registry

import xyz.xenondevs.commons.collections.mapToSet

/**
 * Builds a set of [NovaTagEntry] using the given [build] function.
 */
fun <T : NovaRegistryElement<T>> buildNovaTagEntries(
    build: TagBuilder.Nova<T>.() -> Unit
): Set<NovaTagEntry<T>> = NovaTagBuilderImpl<T>().apply(build).build()

private class NovaTagBuilderImpl<T : NovaRegistryElement<T>> : TagBuilder.Nova<T> {
    
    val entries = mutableSetOf<NovaTagEntry<T>>()
    
    override fun add(entries: Iterable<RegistryEntry.Nova<T>>) {
        this.entries += entries.mapToSet { NovaTagEntry.Direct(it) }
    }
    
    override fun add(tag: RegistryEntrySet.Nova.Tag<T>) {
        this.entries += NovaTagEntry.Tag(tag)
    }
    
    override fun remove(entries: Iterable<RegistryEntry.Nova<T>>) {
        this.entries -= entries.mapToSet { NovaTagEntry.Direct(it) }
    }
    
    override fun remove(tag: RegistryEntrySet.Nova.Tag<T>) {
        this.entries -= NovaTagEntry.Tag(tag)
    }
    
    fun build(): Set<NovaTagEntry<T>> =
        entries.toSet()
    
}