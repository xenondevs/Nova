@file:Suppress("UNCHECKED_CAST", "BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.nova.BOOTSTRAP_LIFECYCLE
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.KnownRegistryEntries.knownRegistryEntries
import xyz.xenondevs.nova.registry.RegistryLoader.enqueueNova
import xyz.xenondevs.nova.registry.RegistryLoader.enqueueVanilla
import xyz.xenondevs.nova.registry.RegistryLoader.novaBuilderFactories
import xyz.xenondevs.nova.registry.RegistryLoader.novaBuilders
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toResourceKey
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * Accepts and queues registrations for both Nova- and Vanilla registries.
 * Responsible for running builders at the correct time during initialization.
 * Unless you're working with a custom [NovaRegistry], prefer registration via [Registrar] (i.e. [Addon]) instead.
 * 
 * Order of operations:
 * * Addons enqueue entry builders via [enqueueNova] and [enqueueVanilla]. (Most likely implicitly through [Registrar]).
 * * All builders (Nova and Vanilla) are invoked and [prepared][RegistryElementBuilder.prepareBuild],
 * e.g. to enqueue asset generation in resource pack tasks.
 * * (pre-world resource pack generation)
 * * Nova registry elements are [built][RegistryElementBuilder.Nova.build]
 * and Nova registries are [frozen][MutableNovaRegistry.freeze].
 * * (rest of pre-world initializables)
 * * Vanilla registry entries are built and injected immediately before Vanilla registries are frozen.
 * * (post-world initializables, including post-world resource pack generation)
 */
@InternalInit(stage = InternalInitStage.PRE_WORLD)
object RegistryLoader {
    
    private val novaBuilderFactories: MutableMap<MutableNovaRegistry<*>, MutableMap<Key, () -> RegistryElementBuilder.Nova<*>>> = HashMap()
    private val vanillaBuilderFactories: MutableMap<RegistryKey<*>, MutableMap<Key, () -> RegistryElementBuilder.Vanilla<*>>> = HashMap()
    private var novaBuilders: MutableMap<MutableNovaRegistry<*>, Map<Key, RegistryElementBuilder.Nova<*>>> = HashMap()
    private val novaRawBuilders: MutableMap<MutableNovaRegistry<*>, MutableMap<Key, (RegistryEntry.Nova<*>) -> NovaRegistryElement<*>>> = HashMap()
    private val novaTagConfigurations: MutableMap<MutableNovaRegistry<*>, MutableMap<Key, MutableList<TagBuilder.Nova<*>.() -> Unit>>> = HashMap()
    private val novaUnknownBuilderFactory: MutableMap<MutableNovaRegistry<*>, (Key) -> RegistryElementBuilder.Nova<*>> = HashMap()
    private val vanillaUnknownBuilderFactory: MutableMap<RegistryKey<*>, (Key) -> RegistryElementBuilder.Vanilla<*>> = HashMap()
    
    /**
     * Enqueues the creation and registration of an [R] in [registry] under [key] by first
     * creating builder via [makeBuilder] and then running it via [runBuilder].
     */
    fun <R : NovaRegistryElement<R>, T, B : RegistryElementBuilder.Nova<T>> enqueueNova(
        registry: MutableNovaRegistry<R>,
        key: Key,
        makeBuilder: (RegistryEntry.Nova<T>) -> B,
        runBuilder: B.() -> Unit
    ): RegistryEntry.Nova<T> where T : R, T : NovaRegistryElement<T> {
        checkFrozen()
        requireKnownNovaRegistry(registry)
        
        val entry = registry[key] as RegistryEntry.Nova<T>
        novaBuilderFactories.getOrPut(registry, ::LinkedHashMap)[key] = {
            val builder = makeBuilder(entry)
            builder.runBuilder()
            builder.prepareBuild()
            builder
        }
        return entry
    }
    
    /**
     * Enqueues the creation and registration of an [R] in [registry] under [key] by building it via [build].
     */
    fun <R : NovaRegistryElement<R>, T> enqueueNova(
        registry: MutableNovaRegistry<R>,
        key: Key,
        build: (RegistryEntry.Nova<T>) -> T
    ): RegistryEntry.Nova<T> where T : R, T : NovaRegistryElement<T> {
        checkFrozen()
        requireKnownNovaRegistry(registry)
        
        novaRawBuilders.getOrPut(registry, ::LinkedHashMap)[key] = build as (RegistryEntry.Nova<*>) -> NovaRegistryElement<*>
        return registry[key] as RegistryEntry.Nova<T>
    }
    
    /**
     * Enqueues the creation and registration of an entry in the Vanilla registry [registry] under [key] by first
     * creating a builder via [makeBuilder] and then running it via [runBuilder].
     */
    fun <T : Keyed, NMS : Any, B : RegistryElementBuilder.Vanilla<NMS>> enqueueVanilla(
        registry: RegistryKey<T>,
        key: Key,
        makeBuilder: (RegistryEntry.Paper<T>) -> B,
        runBuilder: B.() -> Unit
    ): RegistryEntry.Paper<T> {
        checkFrozen()
        
        val entry = RegistryEntry.paper(TypedKey.create(registry, key))
        vanillaBuilderFactories.getOrPut(registry, ::LinkedHashMap)[key] = {
            val builder = makeBuilder(entry)
            builder.runBuilder()
            builder.prepareBuild()
            builder
        }
        return entry
    }
    
    /**
     * Enqueues the addition or modification of a tag in [registry] under [key]
     * with elements provided through [configure].
     */
    fun <T : NovaRegistryElement<T>> enqueueNovaTag(
        registry: MutableNovaRegistry<T>,
        key: Key,
        configure: TagBuilder.Nova<T>.() -> Unit
    ): RegistryEntrySet.Nova.Tag<T> {
        checkFrozen()
        requireKnownNovaRegistry(registry)
        
        novaTagConfigurations
            .getOrPut(registry, ::LinkedHashMap)
            .getOrPut(key, ::ArrayList)
            .add(configure as TagBuilder.Nova<*>.() -> Unit)
        return registry.getTag(key)
    }
    
    /**    
     * Registers a [builder factory][makeBuilder] and [builder configuration][runBuilder] for unknown elements of [registry],
     * which will be invoked for all keys that were registered during a previous iteration but are missing now. 
     * If no unknown builder factory is registered for a registry, missing keys will be ignored.
     */
    fun <T : NovaRegistryElement<T>, B : RegistryElementBuilder.Nova<T>> registerNovaUnknown(
        registry: MutableNovaRegistry<T>,
        makeBuilder: (RegistryEntry.Nova<T>) -> B,
        runBuilder: B.() -> Unit
    ) {
        checkFrozen()
        requireKnownNovaRegistry(registry)
        require(registry !in novaUnknownBuilderFactory) { "Registry $registry already has an unknown builder registered." }
        
        novaUnknownBuilderFactory[registry] = { key ->
            val builder = makeBuilder(registry[key])
            builder.runBuilder()
            builder.prepareBuild()
            builder
        }
    }
    
    /**    
     * Registers a [builder factory][makeBuilder] and [builder configuration][runBuilder] for unknown elements of [registry],
     * which will be invoked for all keys that were registered during a previous iteration but are missing now. 
     * If no unknown builder factory is registered for a registry, missing keys will be ignored.
     */
    fun <T : Keyed, NMS : Any, B : RegistryElementBuilder.Vanilla<NMS>> registerVanillaUnknown(
        registry: RegistryKey<T>,
        makeBuilder: (RegistryEntry.Paper<T>) -> B,
        runBuilder: B.() -> Unit
    ) {
        checkFrozen()
        require(registry !in vanillaUnknownBuilderFactory) { "Registry $registry already has an unknown builder registered." }
        
        vanillaUnknownBuilderFactory[registry] = { key ->
            val entry = RegistryEntry.paper(TypedKey.create(registry, key))
            val builder = makeBuilder(entry)
            builder.runBuilder()
            builder.prepareBuild()
            builder
        }
    }
    
    @InitFun(runBefore = [ResourceGeneration.PreWorld::class])
    private fun prepareBuilders() {
        // prepare nova builders by creating and configuring them
        NovaRegistries.registries.values.forEach(::prepareNovaBuilders)
        
        for ((registryKey, factories) in vanillaBuilderFactories) {
            val factories = factories.toMutableMap()
            val registryResourceKey = registryKey.toResourceKey<Any>()
            
            // add factory for unknown elements
            val unknownBuilderFactory = vanillaUnknownBuilderFactory[registryKey]
            if (unknownBuilderFactory != null) {
                val missingKeys = (knownRegistryEntries[registryKey.key()] ?: emptySet()) - factories.keys
                for (key in missingKeys) {
                    factories[key] = { unknownBuilderFactory(key) }
                }
            }
            
            // enqueue build & registration of entries
            for ((key, factory) in factories) {
                val builder = factory() // prepare builder now
                
                // enqueue build & registration (on nms registry freeze)
                registryResourceKey.preFreeze { registry, lookup ->
                    registry[key] = builder.build(lookup)
                }
                
                // enqueue addition to required tags (on tag build)
                val entry = RegistryEntry.paper(TypedKey.create(registryKey as RegistryKey<Keyed>, key))
                for (tagKey in builder.buildTagSet()) {
                    BOOTSTRAP_LIFECYCLE.modifyTag(tagKey as TagKey<Keyed>) { add(entry) }
                }
            }
            
            // all these keys are now "known" and can become "missing" in the future
            knownRegistryEntries.getOrPut(registryKey.key(), ::HashSet) += factories.keys
        }
    }
    
    /**
     * Prepares nova builder for [registry] by creating and configuring them.
     * This reads from [novaBuilderFactories] and writes to [novaBuilders].
     */
    private fun prepareNovaBuilders(registry: MutableNovaRegistry<*>) {
        novaBuilders[registry] = novaBuilderFactories[registry]?.mapValues { (_, factory) -> factory() } ?: emptyMap()
    }
    
    @InitFun(runAfter = [ResourceGeneration.PreWorld::class])
    private fun runBuilders() {
        NovaRegistries.registries.values.forEach(::runNovaBuilders)
        
        NovaRegistries.freeze()
        LegacyNovaRegistryAccess.freezeAll() // TODO: keep?
    }
    
    /**
     * Runs nova builders for [registry] by building and registering their entries and tags.
     */
    private fun runNovaBuilders(registry: MutableNovaRegistry<*>) {
        registry as MutableNovaRegistry<NovaRegistryElement<*>>
        
        val builders = novaBuilders[registry]?.toMutableMap() ?: mutableMapOf()
        val rawBuilders = novaRawBuilders[registry] ?: emptyMap()
        
        // add factory for unknown elements
        val presentKeys = builders.keys + rawBuilders.keys
        val unknownBuilder = novaUnknownBuilderFactory[registry]
        if (unknownBuilder != null) {
            val missingKeys = (knownRegistryEntries[registry.key] ?: emptySet()) - presentKeys
            for (key in missingKeys) {
                builders[key] = unknownBuilder(key)
            }
        }
        
        // build elements
        for ((key, builder) in builders) {
            registry[key] = builder.build()
        }
        for ((key, build) in rawBuilders) {
            registry[key] = build(registry[key])
        }
        
        // all these keys are now "known" and can become "missing" in the future
        knownRegistryEntries.getOrPut(registry.key, ::HashSet) += presentKeys
        
        // build tags
        for ((key, tagConfigs) in (novaTagConfigurations[registry] ?: emptyMap())) {
            registry[key] = buildNovaTagEntries { tagConfigs.forEach { it() } }
        }
    }
    
    /**
     * [Reloads][MutableNovaRegistry.reload] the given [registry] be re-running the
     * registered builders for its elements and tags.
     */
    fun reload(registry: MutableNovaRegistry<*>) {
        requireKnownNovaRegistry(registry)
        
        registry.reload {
            prepareNovaBuilders(registry)
            runNovaBuilders(registry)
        }
    }
    
    private fun requireKnownNovaRegistry(registry: MutableNovaRegistry<*>) {
        require(registry in NovaRegistries.registries.values) { "Registry ${registry.key} is not a known Nova registry." }
    }
    
    private fun checkFrozen() {
        check(!NovaRegistries.isFrozen) { "Nova registries are frozen." }
    }
    
    @InitFun
    private fun setupReloadAgent() {
        if (!IS_DEV_SERVER)
            return
        
        var hotSwapTask: BukkitTask? = null
        INSTRUMENTATION.addTransformer(
            object : ClassFileTransformer {
                override fun transform(
                    module: Module?,
                    loader: ClassLoader?,
                    className: String?,
                    classBeingRedefined: Class<*>?,
                    protectionDomain: ProtectionDomain?,
                    classfileBuffer: ByteArray?
                ): ByteArray? {
                    if (classBeingRedefined == null)
                        return null
                    
                    hotSwapTask?.cancel()
                    hotSwapTask = runTask {
                        LOGGER.info("Hot swap detected, reloading registries...")
                        val reloadable = NovaRegistries.registries.values.filter { it.isReloadable }
                        val registryCount = reloadable.size
                        reloadable.forEach(::reload)
                        val elementCount = reloadable.sumOf { it.entrySet.get().size }
                        LOGGER.info("Reloaded $registryCount registries with a total of $elementCount elements.")
                        
                        hotSwapTask = null
                    }
                    
                    return null
                }
            },
            true
        )
    }
    
}

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object KnownRegistryEntries {
    
    private const val KNOWN_REGISTRY_ENTRIES_KEY = "known_registry_entries"
    val knownRegistryEntries: MutableMap<Key, MutableSet<Key>> = PermanentStorage.retrieve(KNOWN_REGISTRY_ENTRIES_KEY) ?: HashMap()
    
    @InitFun
    private fun storeKnownRegistryEntryKeys() {
        PermanentStorage.store(KNOWN_REGISTRY_ENTRIES_KEY, knownRegistryEntries)
    }
    
}