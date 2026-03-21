package xyz.xenondevs.nova.registry

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.PreFlattenTagRegistrar
import io.papermc.paper.tag.TagEntry
import org.bukkit.Keyed

/**
 * Registers a pre-flatten [LifecycleEvents.TAGS] that applies [modify] to [tag].
 */
fun <T : Keyed> LifecycleEventManager<BootstrapContext>.modifyTag(
    tag: TagKey<T>,
    priority: Int = 0,
    modify: TagBuilder.Paper<T>.() -> Unit
) {
     registerEventHandler(LifecycleEvents.TAGS.preFlatten(tag.registryKey()).newHandler { event ->
        PaperTagsBuilderImpl(tag).apply(modify).apply(event.registrar())
    }.priority(priority))
}

private class PaperTagsBuilderImpl<T : Keyed>(
    private val tagKey: TagKey<T>
) : TagBuilder.Paper<T> {
    
    private val ops = mutableListOf<TagOperation<T>>()
    
    override fun add(entries: Iterable<RegistryEntry.Paper<T>>) {
        ops += TagOperation.Add(entries.mapTo(LinkedHashSet()) { TagEntry.valueEntry(it.key, true) })
    }
    
    override fun remove(entries: Iterable<RegistryEntry.Paper<T>>) {
        ops += TagOperation.Remove(entries.mapTo(LinkedHashSet()) { TagEntry.valueEntry(it.key, true) })
    }
    
    override fun add(tag: RegistryEntrySet.Paper.Tag<T>) {
        ops += TagOperation.Add(setOf(TagEntry.tagEntry(tag.tagKey, true)))
    }
    
    override fun remove(tag: RegistryEntrySet.Paper.Tag<T>) {
        ops += TagOperation.Remove(setOf(TagEntry.tagEntry(tag.tagKey, true)))
    }
    
    fun apply(registrar: PreFlattenTagRegistrar<T>) {
        val tag = if (registrar.hasTag(tagKey))
            registrar.getTag(tagKey).toMutableSet()
        else mutableSetOf()
        
        for (op in ops) {
            when (op) {
                is TagOperation.Add -> tag += op.entries
                is TagOperation.Remove -> tag -= op.entries
            }
        }
        
        registrar.setTag(tagKey, tag)
    }
    
}

private sealed interface TagOperation<T : Keyed> {
    data class Add<T : Keyed>(val entries: Set<TagEntry<T>>) : TagOperation<T>
    data class Remove<T : Keyed>(val entries: Set<TagEntry<T>>) : TagOperation<T>
}