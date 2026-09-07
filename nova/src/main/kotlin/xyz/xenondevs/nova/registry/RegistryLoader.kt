@file:Suppress("UNCHECKED_CAST", "BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")

package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.TagEntry
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.BOOTSTRAP_LIFECYCLE
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.NovaConfigBackend
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.KnownRegistryEntries.knownRegistryEntries
import xyz.xenondevs.nova.registry.RegistryLoader.novaBuilderFactories
import xyz.xenondevs.nova.registry.RegistryLoader.novaBuilders
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toResourceKey
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

private sealed interface BuilderFactory<E : RegistryEntry<*>, B : RegistryElementBuilder<E>> {
    
    val makeBuilder: (E) -> B
    val runBuilder: (B) -> Unit
    
    fun createAndConfigure(e: E): B
    
    data class Nova<E : NovaRegistryElement<E>, B : RegistryElementBuilder.Nova<E>>(
        override val makeBuilder: (RegistryEntry.Nova<E>) -> B,
        override val runBuilder: B.() -> Unit
    ) : BuilderFactory<RegistryEntry.Nova<E>, B> {
        override fun createAndConfigure(e: RegistryEntry.Nova<E>): B {
            val builder = makeBuilder(e)
            builder.runBuilder()
            builder.prepareBuild()
            return builder
        }
    }
    
    data class Vanilla<API : Keyed, NMS : Any, B : RegistryElementBuilder.Vanilla<API, NMS>>(
        override val makeBuilder: (RegistryEntry.Paper<API>) -> B,
        override val runBuilder: B.() -> Unit
    ) : BuilderFactory<RegistryEntry.Paper<API>, B> {
        override fun createAndConfigure(e: RegistryEntry.Paper<API>): B {
            val builder = makeBuilder(e)
            if (builder is RegistryElementBuilder.RerunnableVanilla<*, *>)
                builder.reset()
            builder.runBuilder()
            builder.prepareBuild()
            return builder
        }
    }
    
}

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
    
    private val novaBuilderFactories: MutableMap<MutableNovaRegistry<*>, MutableMap<Key, BuilderFactory.Nova<*, *>>> = HashMap()
    private var novaBuilders: MutableMap<MutableNovaRegistry<*>, Map<Key, RegistryElementBuilder.Nova<*>>> = HashMap()
    private val novaTagConfigurations: MutableMap<MutableNovaRegistry<*>, MutableMap<Key, MutableList<TagBuilder.Nova<*>.() -> Unit>>> = HashMap()
    
    private val vanillaBuilderFactories: MutableMap<RegistryKey<*>, MutableMap<Key, BuilderFactory.Vanilla<*, *, *>>> = HashMap()
    private val vanillaUnknownBuilderFactory: MutableMap<RegistryKey<*>, BuilderFactory.Vanilla<*, *, *>> = HashMap()
    private val vanillaRerunnableBuilders: MutableMap<RegistryKey<*>, MutableMap<Key, RegistryElementBuilder.RerunnableVanilla<*, *>>> = HashMap()
    
    //<editor-fold desc="registrations">
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
        novaBuilderFactories.getOrPut(registry, ::LinkedHashMap)[key] = BuilderFactory.Nova(makeBuilder, runBuilder)
        return registry[key] as RegistryEntry.Nova<T>
    }
    
    /**
     * Enqueues the creation and registration of an [R] in [registry] under [key] by simply
     * invoking the [build] function.
     */
    fun <R : NovaRegistryElement<R>, T> enqueueNova(
        registry: MutableNovaRegistry<R>,
        key: Key,
        build: (RegistryEntry.Nova<T>) -> T
    ): RegistryEntry.Nova<T> where T : R, T : NovaRegistryElement<T> =
        enqueueNova(registry, key, {}, { entry, _ -> build(entry) })
    
    /**
     * Enqueues the creation and registration of an [R] in [registry] under [key] by using an
     * [anonymous][RegistryElementBuilder.Nova.anonymous] builder to first [prepare] the built,
     * resulting in an intermediary [I], which is then used to build the final value in [build].
     */
    fun <R : NovaRegistryElement<R>, T, I : Any> enqueueNova(
        registry: MutableNovaRegistry<R>,
        key: Key,
        prepare: (RegistryEntry.Nova<T>) -> I,
        build: (RegistryEntry.Nova<T>, I) -> T
    ): RegistryEntry.Nova<T> where T : R, T : NovaRegistryElement<T> {
        checkFrozen()
        requireKnownNovaRegistry(registry)
        novaBuilderFactories.getOrPut(registry, ::LinkedHashMap)[key] = BuilderFactory.Nova<T, RegistryElementBuilder.Nova<T>>(
            { entry -> RegistryElementBuilder.Nova.anonymous(entry, prepare, build) },
            {}
        )
        return registry[key] as RegistryEntry.Nova<T>
    }
    
    /**
     * Enqueues the creation and registration of an entry in the Vanilla registry [registry] under [key] by first
     * creating a builder via [makeBuilder] and then running it via [runBuilder].
     */
    fun <API : Keyed, NMS : Any, B : RegistryElementBuilder.Vanilla<API, NMS>> enqueueVanilla(
        registry: RegistryKey<API>,
        key: Key,
        makeBuilder: (RegistryEntry.Paper<API>) -> B,
        runBuilder: B.() -> Unit
    ): RegistryEntry.Paper<API> {
        checkFrozen()
        vanillaBuilderFactories.getOrPut(registry, ::LinkedHashMap)[key] = BuilderFactory.Vanilla(makeBuilder, runBuilder)
        return RegistryEntry.paper(TypedKey.create(registry, key))
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
    fun <API : Keyed, NMS : Any, B : RegistryElementBuilder.Vanilla<API, NMS>> registerVanillaUnknown(
        registry: RegistryKey<API>,
        makeBuilder: (RegistryEntry.Paper<API>) -> B,
        runBuilder: B.() -> Unit
    ) {
        checkFrozen()
        require(registry !in vanillaUnknownBuilderFactory) { "Registry $registry already has an unknown builder registered." }
        vanillaUnknownBuilderFactory[registry] = BuilderFactory.Vanilla(makeBuilder, runBuilder)
    }
    //</editor-fold>
    
    //<editor-fold desc="lifecycle">
    @InitFun(runBefore = [ResourceGeneration.PreWorld::class])
    private fun prepareBuilders() {
        // prepare nova builders by creating and configuring them
        for ([_, registry] in NovaRegistries.registries) {
            prepareNovaBuilders(registry)
        }
        
        for ([registryKey, factories] in vanillaBuilderFactories) {
            registryKey as RegistryKey<Keyed>
            factories as Map<Key, BuilderFactory.Vanilla<Keyed, Any, *>>
            prepareVanillaBuilders(registryKey, factories)
        }
    }
    
    @InitFun(runAfter = [ResourceGeneration.PreWorld::class])
    private fun runBuilders() {
        for ([_, registry] in NovaRegistries.registries) {
            runNovaBuilders(registry)
        }
        
        NovaRegistries.freeze()
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
                        
                        // reload nova registries
                        val reloadable = NovaRegistries.registries.values.filter { it.isReloadable }
                        reloadable.forEach(::reload)
                        val reloadRegistryCount = reloadable.size
                        val reloadElementCount = reloadable.sumOf { it.entrySet.get().size }
                        
                        // rerun vanilla builders
                        val rerunnable = vanillaRerunnableBuilders.keys
                        rerunnable.forEach(::rerun)
                        val rerunRegistryCount = rerunnable.size
                        val rerunElementCount = vanillaRerunnableBuilders.values.sumOf(Map<*, *>::size)
                        
                        NovaConfigBackend.postReload()
                        
                        LOGGER.info("Reloaded $reloadRegistryCount registries with a total of $reloadElementCount elements.")
                        LOGGER.info("Re-ran builders for $rerunRegistryCount registries totaling $rerunElementCount elements")
                        
                        hotSwapTask = null
                    }
                    
                    return null
                }
            },
            true
        )
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
    
    /**
     * Re-runs the registered builders for [registry] without reloading the registry itself.
     * This is a hack to implement something akin to registry reloading for vanilla registries.
     * It is up to the registry element builder to make sure that re-running it propagates the changes.
     */
    fun rerun(registry: RegistryKey<*>) {
        check(!RegistryContext.isInBootstrapPhase) // re-running is not for bootstrap phase
        
        vanillaRerunnableBuilders[registry]?.forEach { [key, builder] ->
            val factory = vanillaBuilderFactories[registry]?.get(key)
                ?: return@forEach
            factory as BuilderFactory.Vanilla<Keyed, Any, RegistryElementBuilder.RerunnableVanilla<Keyed, Any>>
            builder.reset()
            factory.runBuilder(builder)
            builder.prepareBuild()
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="lifecycle impl vanilla">
    private fun <API : Keyed, NMS : Any> prepareVanillaBuilders(
        registryKey: RegistryKey<API>,
        registeredFactories: Map<Key, BuilderFactory.Vanilla<API, NMS, *>>
    ) {
        val factories = registeredFactories.toMutableMap()
        val registryResourceKey = registryKey.toResourceKey<NMS>()
        
        // add factory for unknown elements
        val unknownBuilderFactory = vanillaUnknownBuilderFactory[registryKey] as BuilderFactory.Vanilla<API, NMS, *>?
        if (unknownBuilderFactory != null) {
            val missingKeys = (knownRegistryEntries[registryKey.key()] ?: emptySet()) - factories.keys
            for (key in missingKeys) {
                factories[key] = unknownBuilderFactory
            }
        }
        
        // enqueue build & registration of entries
        val rerunnableBuilders = vanillaRerunnableBuilders.getOrPut(registryKey, ::LinkedHashMap)
        val builders = ArrayList<RegistryElementBuilder.Vanilla<API, NMS>>()
        for ([key, factory] in factories) {
            val entry = RegistryEntry.paper(TypedKey.create(registryKey, key))
            val builder = factory.createAndConfigure(entry)
            builders += builder
            if (key in registeredFactories && builder is RegistryElementBuilder.RerunnableVanilla<*, *>)
                rerunnableBuilders[key] = builder
            
            // enqueue build & registration (on nms registry freeze)
            registryResourceKey.preFreeze { registry, lookup ->
                registry[key] = builder.build(lookup)
            }
        }
        
        // implicit tags entries from builders
        val implicitEntries: Provider<Map<TagKey<API>, Set<TagEntry<API>>>> = combinedProvider(
            builders.map { builder -> builder.tags.map { builder.entry to it } }
        ) { tagsPerEntry ->
            buildMap<TagKey<API>, MutableSet<TagEntry<API>>> {
                for ([entry, tagsDefinedInBuilder] in tagsPerEntry) {
                    for (tag in tagsDefinedInBuilder) {
                        require(tag.registry == registryKey) { "Cannot add $entry to tag $tag from another registry" }
                        getOrPut(tag.tagKey, ::LinkedHashSet) += TagEntry.valueEntry(TypedKey.create(registryKey, entry.key), true)
                    }
                }
            }
        }
        BOOTSTRAP_LIFECYCLE.addToTags(registryKey, implicitEntries)
        
        // all these keys are now "known" and can become "missing" in the future
        knownRegistryEntries.getOrPut(registryKey.key(), ::HashSet) += factories.keys
    }
    //</editor-fold>
    
    //<editor-fold desc="lifecycle impl nova">
    /**
     * Prepares nova builder for [registry] by creating and configuring them.
     * This reads from [novaBuilderFactories] and writes to [novaBuilders].
     */
    private fun <T : NovaRegistryElement<T>> prepareNovaBuilders(registry: MutableNovaRegistry<T>) {
        // pass known keys to registry so it can use its unknown factory for missing keys
        knownRegistryEntries[registry.key]?.forEach(registry::setKnown)
        
        novaBuilders[registry] = novaBuilderFactories[registry]?.mapValues { [key, factory] ->
            factory as BuilderFactory.Nova<T, *>
            factory.createAndConfigure(registry[key])
        } ?: emptyMap()
    }
    
    /**
     * Runs nova builders for [registry] by building and registering their entries and tags.
     */
    private fun <T : NovaRegistryElement<T>> runNovaBuilders(registry: MutableNovaRegistry<T>) {
        val builders = (novaBuilders[registry]?.toMutableMap() ?: mutableMapOf())
            as MutableMap<Key, RegistryElementBuilder.Nova<T>>
        
        // build elements
        for ([key, builder] in builders) {
            registry[key] = builder.build()
        }
        
        // all these keys are now "known" and can become "missing" in the future
        knownRegistryEntries.getOrPut(registry.key, ::HashSet) += builders.keys
        
        // build tags
        val tagConfigurations = novaTagConfigurations[registry] as Map<Key, List<TagBuilder.Nova<T>.() -> Unit>>?
            ?: emptyMap()
        val implicitEntries: Provider<Map<Key, Set<RegistryEntry.Nova<T>>>> = combinedProvider(
            builders.map { [_, builder] -> builder.tags.map { builder.entry to it } }
        ) { extraTagsPerEntry ->
            buildMap<Key, MutableSet<RegistryEntry.Nova<T>>> {
                for ([entry, tagsDefinedInBuilder] in extraTagsPerEntry) {
                    for (tag in tagsDefinedInBuilder) {
                        if (tag.registry != registry || tag.tagKey !in tagConfigurations)
                            continue
                        getOrPut(tag.tagKey, ::LinkedHashSet) += entry
                    }
                }
            }
        }
        for ([tagKey, configurations] in tagConfigurations) {
            registry[tagKey] = buildNovaTagEntries {
                add(implicitEntries.map { it[tagKey] ?: emptySet() })
                configurations.forEach { it() }
            }
        }
    }
    
    private fun requireKnownNovaRegistry(registry: MutableNovaRegistry<*>) {
        require(registry in NovaRegistries.registries.values) { "Registry ${registry.key.asString()} is not a known Nova registry." }
    }
    
    private fun checkFrozen() {
        check(!NovaRegistries.isFrozen) { "Nova registries are frozen." }
    }
    //</editor-fold>
    
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