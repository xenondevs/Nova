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
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.ui.menu.setGlobalIngredients
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.data.JarUtils
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path

private val LOGGING by MAIN_CONFIG.entry<Boolean>("debug", "logging", "initializer")

internal object Initializer : Listener {
    
    private val initializables = HashSet<Initializable>()
    private val disableables = HashSet<DisableableFunction>()
    
    private val initPreWorld = DirectedAcyclicGraph<Initializable, DefaultEdge>(DefaultEdge::class.java)
    private val initPostWorld = DirectedAcyclicGraph<Initializable, DefaultEdge>(DefaultEdge::class.java)
    private val disable = DirectedAcyclicGraph<DisableableFunction, DefaultEdge>(DefaultEdge::class.java)
    
    private var preWorldInitialized = false
    var isDone = false
        private set
    
    /**
     * Stats the initialization process.
     */
    suspend fun start() = tryInit {
        collectAndRegisterRunnables(NOVA_JAR, this.javaClass.classLoader)
        for (addon in AddonBootstrapper.addons) {
            collectAndRegisterRunnables(addon.file, addon.javaClass.classLoader)
        }
        
        initPreWorld()
    }
    
    private fun collectAndRegisterRunnables(file: Path, classLoader: ClassLoader) {
        val [initializables, disableables] = collectRunnables(file, classLoader)
        addRunnables(initializables, disableables)
    }
    
    /**
     * Searches [file] and collects classes annotated by [InternalInit] and [Init] and functions
     * annotated by [InitFun] and [DisableFun] as [Initializables][Initializable] and [DisableableFunctions][DisableableFunction].
     */
    private fun collectRunnables(file: Path, classLoader: ClassLoader): Pair<List<Initializable>, List<DisableableFunction>> {
        val initializables = ArrayList<Initializable>()
        val disableables = ArrayList<DisableableFunction>()
        val index = JarUtils.getAnnotationIndex(file)
        val internalInit = Type.getDescriptor(InternalInit::class.java)
        val init = Type.getDescriptor(Init::class.java)
        val initFun = Type.getDescriptor(InitFun::class.java)
        val disableFun = Type.getDescriptor(DisableFun::class.java)
        
        for ([className, metadata] in index.classes) {
            var clazz: InitializableClass? = null
            metadata.annotations.firstOrNull { it.descriptor == internalInit }?.let {
                clazz = InitializableClass.fromInternalAnnotation(classLoader, className, it).also(initializables::add)
            }
            metadata.annotations.firstOrNull { it.descriptor == init }?.let {
                clazz = InitializableClass.fromAddonAnnotation(classLoader, className, it).also(initializables::add)
            }
            
            for (method in metadata.methods) {
                method.annotations.firstOrNull { it.descriptor == initFun }?.let {
                    val initClass = clazz ?: throw IllegalStateException("Class $className is missing an init annotation!")
                    initializables += InitializableFunction.fromInitAnnotation(initClass, method, it)
                }
                method.annotations.firstOrNull { it.descriptor == disableFun }?.let {
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
        check(!preWorldInitialized) { "Cannot add additional callables after pre-world initialization!" }
        
        // add vertices
        for (initializable in initializables) {
            this.initializables += initializable
            when (initializable.stage) {
                InternalInitStage.PRE_WORLD -> initPreWorld.addVertex(initializable)
                else -> initPostWorld.addVertex(initializable)
            }
        }
        for (disableable in disableables) {
            this.disableables += disableable
            disable.addVertex(disableable)
        }
        
        // add edges
        for (initializable in initializables) {
            initializable.loadDependencies(
                this.initializables,
                if (initializable.stage == InternalInitStage.PRE_WORLD) initPreWorld else initPostWorld
            )
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
        val preWorldFile = File(dir, "pre_world.dot")
        val postWorldFile = File(dir, "post_world.dot")
        val disableFile = File(dir, "disable.dot")
        dir.mkdirs()
        
        val exporter = DOTExporter<InitializerRunnable<*>, DefaultEdge>()
        exporter.setVertexAttributeProvider { vertex ->
            mapOf(
                "label" to DefaultAttribute.createAttribute(vertex.toString()),
                "color" to DefaultAttribute.createAttribute(if (vertex.dispatcher != null) "aqua" else "black")
            )
        }
        exporter.exportGraph(initPreWorld as Graph<InitializerRunnable<*>, DefaultEdge>, preWorldFile)
        exporter.exportGraph(initPostWorld as Graph<InitializerRunnable<*>, DefaultEdge>, postWorldFile)
        exporter.exportGraph(disable as Graph<InitializerRunnable<*>, DefaultEdge>, disableFile)
    }
    
    /**
     * Stats the pre-world initialization process.
     */
    private suspend fun initPreWorld() {
        tryInit {
            coroutineScope {
                launchAll(this, initPreWorld)
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
                launchAll(this, initPostWorld)
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
     * Returns whether the initialization was successful, and also shuts down the server if it wasn't.
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