@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.patch.impl.registry

import io.papermc.paper.registry.data.util.Conversions
import io.papermc.paper.tag.TagEventConfig
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.RegistryDataLoader
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagKey
import net.minecraft.tags.TagLoader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toResourceLocation

private val LOG_REGISTRY_FREEZE by MAIN_CONFIG.entry<Boolean>("debug", "logging", "registry_freeze")

private val BUILTIN_REGISTRIES_FREEZE = ReflectionUtils.getMethod(BuiltInRegistries::class, "freeze")
private val REGISTRY_DATA_LOADER_LOADER = ReflectionUtils.getClass("net.minecraft.resources.RegistryDataLoader\$Loader").kotlin
private val REGISTRY_DATA_LOADER_LOAD_FOR_EACH = ReflectionUtils.getMethod(
    RegistryDataLoader::class, true, "lambda\$load\$6",
    Conversions::class, Map::class, REGISTRY_DATA_LOADER_LOADER
)

private typealias PreFreezeListener<T> = (registry: WritableRegistry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit
private typealias PostFreezeListener<T> = (registry: Registry<T>, lookup: RegistryOps.RegistryInfoLookup) -> Unit

internal object RegistryEventsPatch : MultiTransformer(BuiltInRegistries::class, RegistryDataLoader::class, TagLoader::class) {
    
    private val preFreezeListeners = HashMap<ResourceKey<*>, ArrayList<PreFreezeListener<*>>>()
    private val postFreezeListeners = HashMap<ResourceKey<*>, ArrayList<PostFreezeListener<*>>>()
    private val frozen = HashSet<ResourceKey<*>>()
    
    private val additionalTagEntries = HashMap<ResourceKey<*>, HashMap<TagKey<*>, ArrayList<ResourceLocation>>>()
    
    override fun transform() {
        VirtualClassPath[BUILTIN_REGISTRIES_FREEZE].replaceEvery(
            0, 0,
            {
                dup()
                getStatic(BuiltInRegistries::BUILT_IN_CONVERSIONS)
                invokeStatic(::handlePreFreeze)
                dup()
                invokeInterface(Registry<*>::freeze)
                getStatic(BuiltInRegistries::BUILT_IN_CONVERSIONS)
                invokeStatic(::handlePostFreeze)
            }
        ) { it.opcode == Opcodes.INVOKEINTERFACE && (it as MethodInsnNode).calls(Registry<*>::freeze) }
        
        VirtualClassPath[REGISTRY_DATA_LOADER_LOAD_FOR_EACH].replaceEvery(
            0, 0,
            {
                dup()
                aLoad(0) // Conversions
                invokeStatic(::handlePreFreeze)
                dup()
                invokeInterface(Registry<*>::freeze)
                aLoad(0) // Conversions
                invokeStatic(::handlePostFreeze)
            }
        ) { it.opcode == Opcodes.INVOKEINTERFACE && (it as MethodInsnNode).calls(Registry<*>::freeze) }
        
        VirtualClassPath[TagLoader<*>::build].instructions.insert(buildInsnList {
            addLabel()
            aLoad(1) // tag map
            aLoad(2) // paper event config
            invokeStatic(::handleTagsBuild)
        })
    }
    
    @JvmStatic
    fun handlePreFreeze(registry: WritableRegistry<*>, conversions: Conversions) {
        val key = registry.key()
        val lookup = conversions.lookup()
        try {
            preFreezeListeners[key]?.forEach { it(registry, lookup) }
            preFreezeListeners.remove(key)
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while running registry pre-freeze listeners for $key", t)
        }
        
        if (LOG_REGISTRY_FREEZE) {
            LOGGER.info("Freezing registry $key")
        }
    }
    
    @JvmStatic
    fun handlePostFreeze(registry: Registry<*>, conversions: Conversions) {
        val key = registry.key()
        val lookup = conversions.lookup()
        try {
            postFreezeListeners[key]?.forEach { it(registry, lookup) }
            postFreezeListeners.remove(key)
        } catch (t: Throwable) {
            LOGGER.error("An exception occurred while running registry post-freeze listeners for $key", t)
        }
    }
    
    @JvmStatic
    fun handleTagsBuild(
        map: MutableMap<ResourceLocation, MutableList<TagLoader.EntryWithSource>>, // Map<Tag ID, List<Entry ID / other Tag ID>>
        config: TagEventConfig<*, *>?
    ) {
        if (config == null)
            return
        
        val key = ResourceKey.createRegistryKey<Any>(config.apiRegistryKey().key().toResourceLocation())
        val additionalEntriesForRegistry = additionalTagEntries[key]
            ?: return
        
        for ((tagKey, entries) in additionalEntriesForRegistry) {
            val mappedEntries = entries.map { TagLoader.EntryWithSource(TagEntry.element(it), "Nova") }
            map.getOrPut(tagKey.location, ::ArrayList) += mappedEntries
        }
    }
    
    fun <T> addPreFreezeListener(key: ResourceKey<out Registry<T>>, listener: PreFreezeListener<T>) {
        check(key !in frozen) { "Registry $key is already frozen!" }
        preFreezeListeners.getOrPut(key, ::ArrayList) += listener as PreFreezeListener<*>
    }
    
    fun <T> addPostFreezeListener(key: ResourceKey<out Registry<T>>, listener: PostFreezeListener<T>) {
        check(key !in frozen) { "Registry $key is already frozen!" }
        postFreezeListeners.getOrPut(key, ::ArrayList) += listener as PostFreezeListener<*>
    }
    
    fun addTagEntry(key: TagKey<*>, value: ResourceLocation) {
        additionalTagEntries
            .getOrPut(key.registry(), ::HashMap)
            .getOrPut(key, ::ArrayList) += value
    }
    
}

internal fun <T> ResourceKey<out Registry<T>>.preFreeze(listener: PreFreezeListener<T>) {
    RegistryEventsPatch.addPreFreezeListener(this, listener)
}

internal fun <T> ResourceKey<out Registry<T>>.postFreeze(listener: PostFreezeListener<T>) {
    RegistryEventsPatch.addPostFreezeListener(this, listener)
}

// kotlin seems to not be able to resolve the above function if there's no T, even if registry is unused
internal fun ResourceKey<out Registry<*>>.preFreeze(listener: (lookup: RegistryOps.RegistryInfoLookup) -> Unit) {
    (this as ResourceKey<Registry<Any>>).preFreeze { _, lookup -> listener(lookup) }
}

internal operator fun <T : Any> ResourceKey<out Registry<T>>.set(id: ResourceLocation, value: T) {
    preFreeze { registry, _ -> registry[id] = value }
}

internal operator fun <T : Any> ResourceKey<out Registry<T>>.set(id: ResourceKey<T>, value: T) {
    preFreeze { registry, _ -> registry[id] = value }
}

internal operator fun TagKey<*>.plusAssign(id: ResourceLocation) {
    RegistryEventsPatch.addTagEntry(this, id)
}