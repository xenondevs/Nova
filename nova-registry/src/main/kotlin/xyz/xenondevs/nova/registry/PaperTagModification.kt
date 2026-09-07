@file:Suppress("INAPPLICABLE_JVM_NAME")

package xyz.xenondevs.nova.registry

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.PreFlattenTagRegistrar
import io.papermc.paper.tag.TagEntry
import org.bukkit.Bukkit
import org.bukkit.Keyed
import xyz.xenondevs.commons.collections.mapToSet
import xyz.xenondevs.commons.provider.Provider

/**
 * Registers a pre-flatten [LifecycleEvents.TAGS] handler that applies [modify] to [tag].
 * The builder is run again on every tag reload.
 * Changes to providers supplied to this builder cause a [data reload][Bukkit.reloadData]. 
 */
fun <T : Keyed> LifecycleEventManager<BootstrapContext>.modifyTag(
    tag: TagKey<T>,
    priority: Int = 0,
    modify: TagBuilder.Paper<T>.() -> Unit
) {
    val builder = PaperTagBuilder(tag, modify)
    registerEventHandler(
        LifecycleEvents.TAGS.preFlatten(tag.registryKey())
            .newHandler { event -> builder.apply(event.registrar()) }
            .priority(priority)
    )
}

/**
 * Adds the entries in [entriesByTag] to their corresponding tags during pre-flatten.
 * Changes to [entriesByTag] cause a [data reload][Bukkit.reloadData].
 */
fun <T : Keyed> LifecycleEventManager<BootstrapContext>.addToTags(
    registry: RegistryKey<T>,
    entriesByTag: Provider<Map<TagKey<T>, Set<TagEntry<T>>>>,
    priority: Int = -1
) {
    entriesByTag.observe(RegistryContext::scheduleDataReload)
    registerEventHandler(
        LifecycleEvents.TAGS.preFlatten(registry)
            .newHandler { event ->
                val registrar = event.registrar()
                for ([tag, entries] in entriesByTag.get()) {
                    require(tag.registryKey() == registry) { "Cannot modify tag $tag from another registry" }
                    val contents = if (registrar.hasTag(tag))
                        registrar.getTag(tag).toMutableSet()
                    else mutableSetOf()
                    contents += entries
                    registrar.setTag(tag, contents)
                }
            }
            .priority(priority)
    )
}

private class PaperTagBuilder<T : Keyed>(
    private val tag: TagKey<T>,
    private val modify: TagBuilder.Paper<T>.() -> Unit
) : TagBuilder.Paper<T> {
    
    private val operations = ArrayList<PaperTagOperation<T>>()
    private val observedProviders = ArrayList<Provider<Set<TagEntry<T>>>>()
    private val scheduleDataReload: () -> Unit = RegistryContext::scheduleDataReload
    
    @JvmName("addEntriesProvider")
    override fun add(entries: Provider<Iterable<RegistryEntry.Paper<T>>>) {
        addOperation(
            entries.map { values ->
                values.mapToSet { TagEntry.valueEntry(TypedKey.create(tag.registryKey(), it.key), true) }
            },
            PaperTagOperation<T>::Add
        )
    }
    
    @JvmName("removeEntriesProvider")
    override fun remove(entries: Provider<Iterable<RegistryEntry.Paper<T>>>) {
        addOperation(
            entries.map { values ->
                values.mapToSet { TagEntry.valueEntry(TypedKey.create(tag.registryKey(), it.key), true) }
            },
            PaperTagOperation<T>::Remove
        )
    }
    
    @JvmName("addTagProvider")
    override fun add(tag: Provider<RegistryEntrySet.Paper.Tag<T>>) {
        addOperation(
            tag.map { setOf(TagEntry.tagEntry(it.tagKey, true)) },
            PaperTagOperation<T>::Add
        )
    }
    
    @JvmName("removeTagProvider")
    override fun remove(tag: Provider<RegistryEntrySet.Paper.Tag<T>>) {
        addOperation(
            tag.map { setOf(TagEntry.tagEntry(it.tagKey, true)) },
            PaperTagOperation<T>::Remove
        )
    }
    
    private fun addOperation(
        entries: Provider<Set<TagEntry<T>>>,
        operation: (Set<TagEntry<T>>) -> PaperTagOperation<T>
    ) {
        operations += operation(entries.get())
        if (!entries.isStable)
            observedProviders += entries
    }
    
    fun apply(registrar: PreFlattenTagRegistrar<T>) {
        observedProviders.forEach { it.unobserve(scheduleDataReload) }
        operations.clear()
        observedProviders.clear()
        modify()
        observedProviders.forEach { it.observe(scheduleDataReload) }
        
        val entries = if (registrar.hasTag(tag))
            registrar.getTag(tag).toMutableSet()
        else mutableSetOf()
        
        for (operation in operations) {
            when (operation) {
                is PaperTagOperation.Add -> entries += operation.entries
                is PaperTagOperation.Remove -> entries -= operation.entries
            }
        }
        
        registrar.setTag(tag, entries)
    }
    
}

private sealed interface PaperTagOperation<T : Keyed> {
    
    val entries: Set<TagEntry<T>>
    
    data class Add<T : Keyed>(
        override val entries: Set<TagEntry<T>>
    ) : PaperTagOperation<T>
    
    data class Remove<T : Keyed>(
        override val entries: Set<TagEntry<T>>
    ) : PaperTagOperation<T>
    
}
