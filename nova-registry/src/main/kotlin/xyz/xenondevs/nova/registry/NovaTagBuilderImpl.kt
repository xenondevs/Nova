@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider

/**
 * Builds a provider of [NovaTagEntry] sets using the given [build] function.
 */
fun <T : NovaRegistryElement<T>> buildNovaTagEntries(
    build: TagBuilder.Nova<T>.() -> Unit
): Provider<Set<NovaTagEntry<T>>> = NovaTagBuilderImpl<T>().apply(build).build()

private class NovaTagBuilderImpl<T : NovaRegistryElement<T>> : TagBuilder.Nova<T> {
    
    private val operations = mutableListOf<NovaTagOperation<T>>()
    
    @JvmName("addEntriesProvider")
    override fun add(entries: Provider<Iterable<RegistryEntry.Nova<T>>>) {
        operations += NovaTagOperation.Add(entries.map { values ->
            values.mapToSet { NovaTagEntry.Direct(it) }
        })
    }
    
    @JvmName("addTagProvider")
    override fun add(tag: Provider<RegistryEntrySet.Nova.Tag<T>>) {
        operations += NovaTagOperation.Add(tag.map { setOf(NovaTagEntry.Tag(it)) })
    }
    
    @JvmName("removeEntriesProvider")
    override fun remove(entries: Provider<Iterable<RegistryEntry.Nova<T>>>) {
        operations += NovaTagOperation.Remove(entries.map { values ->
            values.mapToSet { NovaTagEntry.Direct(it) }
        })
    }
    
    @JvmName("removeTagProvider")
    override fun remove(tag: Provider<RegistryEntrySet.Nova.Tag<T>>) {
        operations += NovaTagOperation.Remove(tag.map { setOf(NovaTagEntry.Tag(it)) })
    }
    
    fun build(): Provider<Set<NovaTagEntry<T>>> {
        if (operations.isEmpty())
            return provider(emptySet())
        
        return combinedProvider(operations.map { it.entries }) { entriesByOperation ->
            buildSet {
                for (i in operations.indices) {
                    when (operations[i]) {
                        is NovaTagOperation.Add -> addAll(entriesByOperation[i])
                        is NovaTagOperation.Remove -> removeAll(entriesByOperation[i])
                    }
                }
            }
        }
    }
    
}

private sealed interface NovaTagOperation<T : NovaRegistryElement<T>> {
    
    val entries: Provider<Set<NovaTagEntry<T>>>
    
    data class Add<T : NovaRegistryElement<T>>(
        override val entries: Provider<Set<NovaTagEntry<T>>>
    ) : NovaTagOperation<T>
    
    data class Remove<T : NovaRegistryElement<T>>(
        override val entries: Provider<Set<NovaTagEntry<T>>>
    ) : NovaTagOperation<T>
    
}