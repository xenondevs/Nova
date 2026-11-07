package xyz.xenondevs.nova.initialize

import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.bstats.bukkit.Metrics
import org.bstats.charts.DrilldownPie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import org.objectweb.asm.Type
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.addon.version
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.ui.menu.setGlobalIngredients
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.data.JarUtils
import xyz.xenondevs.nova.util.registerEvents
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path

private val LOGGING by MAIN_CONFIG.entry<Boolean>("debug", "logging", "initializer")

internal object Initializer : Listener {
    
    private val initializables = HashSet<Initializable>()
    private val disableables = HashSet<DisableableFunction>()
    
    private val init = InternalInitStage.entries.associateWith { DirectedAcyclicGraph<Initializable, DefaultEdge>(DefaultEdge::class.java) }
    private val disable = DirectedAcyclicGraph<DisableableFunction, DefaultEdge>(DefaultEdge::class.java)
    
    private var bootstrapInitialized = false
    private var preWorldInitialized = false
    var isDone = false
        private set
    
    /**
     * Collects all initialization tasks and runs the bootstrap phase.
     */
    suspend fun start() {
        tryInit {
            collectAndRegisterRunnables(NOVA_JAR, this.javaClass.classLoader)
            for (addon in AddonBootstrapper.addons) {
                collectAndRegisterRunnables(addon.file, addon.javaClass.classLoader)
            }
        }
        
        initBootstrap()
    }
    
    private fun collectAndRegisterRunnables(file: Path, classLoader: ClassLoader) {
        val [initializables, disableables] = collectRunnables(JarUtils.getAnnotationIndex(file), classLoader)
        addRunnables(initializables, disableables)
    }
    
    private fun collectRunnables(
        index: AnnotationIndex,
        classLoader: ClassLoader
    ): Pair<List<Initializable>, List<DisableableFunction>> {
        val initializables = ArrayList<Initializable>()
        val disableables = ArrayList<DisableableFunction>()
        val initDescriptors = setOf(Type.getDescriptor(Init::class.java), Type.getDescriptor(InternalInit::class.java))
        val disableDescriptor = Type.getDescriptor(DisableFun::class.java)
        @Suppress("DEPRECATION")
        val legacyInitDescriptor = Type.getDescriptor(InitFun::class.java)
        
        fun collectInit(className: String, method: AnnotationIndex.Method?, annotations: List<AnnotationIndex.Annotation>): Initializable? {
            val initAnnotations = annotations.filter { it.descriptor in initDescriptors }
            require(initAnnotations.size <= 1) { "Multiple initialization annotations on $className${method?.let { "::${it.name}" } ?: ""}" }
            return initAnnotations.singleOrNull()?.let {
                Initializable.fromAnnotation(classLoader, className, method, it).also(initializables::add)
            }
        }
        
        for ([className, metadata] in index.classes) {
            val clazz = collectInit(className, null, metadata.annotations)
            for (method in metadata.methods) {
                val function = collectInit(className, method, method.annotations)
                method.annotations.firstOrNull { it.descriptor == legacyInitDescriptor }?.let {
                    require(function == null) { "Multiple initialization annotations on $className::${method.name}" }
                    val initClass = requireNotNull(clazz) { "Class $className is missing an init annotation required by @InitFun!" }
                    initializables += Initializable.fromLegacyAnnotation(initClass, method, it)
                }
                method.annotations.firstOrNull { it.descriptor == disableDescriptor }?.let {
                    disableables += DisableableFunction.fromInitAnnotation(classLoader, className, method, it)
                }
            }
        }
        
        return initializables to disableables
    }
    
    /**
     * Adds the given [Initializables][Initializable] and [DisableableFunctions][DisableableFunction] to the initialization process.
     */
    private fun addRunnables(initializables: List<Initializable>, disableables: List<DisableableFunction>) {
        check(!bootstrapInitialized) { "Cannot add additional callables after bootstrap initialization!" }
        
        // add vertices
        for (initializable in initializables) {
            this.initializables += initializable
            init.getValue(initializable.stage).addVertex(initializable)
        }
        for (disableable in disableables) {
            this.disableables += disableable
            disable.addVertex(disableable)
        }
        
        // add edges
        for (initializable in initializables) {
            initializable.loadDependencies(this.initializables, init.getValue(initializable.stage))
        }
        for (disableable in disableables) {
            disableable.loadDependencies(this.disableables, disable)
        }
        
        if (IS_DEV_SERVER)
            dumpGraphs()
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun dumpGraphs() {
        val dir = File("debug/nova/")
        val disableFile = File(dir, "disable.dot")
        dir.mkdirs()
        
        val exporter = DOTExporter<InitializerRunnable<*>, DefaultEdge>()
        exporter.setVertexAttributeProvider { vertex ->
            mapOf(
                "label" to DefaultAttribute.createAttribute(vertex.toString()),
                "color" to DefaultAttribute.createAttribute(if (vertex.dispatcher != null) "aqua" else "black")
            )
        }
        for ((key, value) in init) {
            exporter.exportGraph(value as Graph<InitializerRunnable<*>, DefaultEdge>, File(dir, "${key.name.lowercase()}.dot"))
        }
        exporter.exportGraph(disable as Graph<InitializerRunnable<*>, DefaultEdge>, disableFile)
    }
    
    /**
     * Runs the bootstrap phase after collecting all initialization tasks.
     */
    private suspend fun initBootstrap() {
        tryInit {
            check(!bootstrapInitialized) { "Bootstrap initialization has already completed!" }
            coroutineScope {
                launchAll(this, init.getValue(InternalInitStage.BOOTSTRAP))
            }
        }
        
        bootstrapInitialized = true
    }
    
    @InternalInit(stage = PRE_WORLD)
    private fun registerListener() {
        registerEvents()
    }
    
    /**
     * Runs the pre-world phase during Nova's plugin enable.
     */
    suspend fun initPreWorld() {
        tryInit {
            check(bootstrapInitialized) { "Cannot start pre-world initialization before bootstrap initialization!" }
            check(!preWorldInitialized) { "Pre-world initialization has already completed!" }
            coroutineScope {
                launchAll(this, init.getValue(InternalInitStage.PRE_WORLD))
            }
        }
        
        preWorldInitialized = true
    }
    
    /**
     * Starts the post-world initialization process.
     */
    private fun initPostWorld() = runBlocking {
        tryInit {
            coroutineScope {
                launchAll(this, init.getValue(InternalInitStage.POST_WORLD))
            }
        }
        
        isDone = true
        callEvent(NovaLoadDataEvent())
        
        @Suppress("UnstableApiUsage")
        PermanentStorage.store("last_version", Nova.pluginMeta.version)
        setGlobalIngredients()
        setupMetrics()
        LOGGER.info("Done loading")
    }
    
    /**
     * Launches all vertices of [graph] in the given [scope].
     */
    private fun <T : InitializerRunnable<T>> launchAll(scope: CoroutineScope, graph: Graph<T, DefaultEdge>) {
        for (initializable in graph.vertexSet()) {
            launch(scope, initializable, graph)
        }
    }
    
    /**
     * Launches [runnable] of [graph] in the given [scope].
     */
    private fun <T : InitializerRunnable<T>> launch(
        scope: CoroutineScope,
        runnable: T,
        graph: Graph<T, DefaultEdge>
    ) {
        scope.launch {
            // await dependencies
            graph.incomingEdgesOf(runnable)
                .map { graph.getEdgeSource(it).completion }
                .awaitAll()
            
            // run in preferred context
            withContext(runnable.dispatcher ?: scope.coroutineContext) {
                if (LOGGING)
                    LOGGER.info(runnable.toString())
                
                runnable.run()
            }
        }
    }
    
    /**
     * Wraps [run] in a try-catch block with error logging specific to initialization.
     * Shuts down the server if initialization fails.
     */
    private suspend inline fun tryInit(run: () -> Unit) {
        try {
            run()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val cause = if (t is InvocationTargetException) t.targetException else t
            if (cause is InitializationException) {
                LOGGER.error(cause.message)
            } else {
                LOGGER.error("An exception occurred during initialization", cause)
            }
            
            LOGGER.error("Initialization failure")
            PermanentStorage.shutdownAndWait()
            (LogManager.getContext(false) as LoggerContext).stop() // flush log messages
            Runtime.getRuntime().halt(-1) // force-quit process to prevent further errors
        }
    }
    
    /**
     * Disables all [Initializables][Initializable] in the reverse order that they were initialized in.
     */
    suspend fun disable() {
        if (isDone) {
            coroutineScope { launchAll(this, disable) }
        } else {
            LOGGER.warn("Skipping disable phase due to incomplete initialization")
        }
    }
    
    @EventHandler
    private fun handleServerStarted(event: ServerLoadEvent) {
        if (preWorldInitialized) {
            initPostWorld()
        } else LOGGER.warn("Skipping post world initialization")
    }
    
    private fun setupMetrics() {
        val metrics = Metrics(Nova, 11927)
        metrics.addCustomChart(DrilldownPie("addons") {
            val map = HashMap<String, Map<String, Int>>()
            
            for (addon in AddonBootstrapper.addons) {
                map[addon.name] = mapOf(addon.version to 1)
            }
            
            return@DrilldownPie map
        })
    }
    
}