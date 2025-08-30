package xyz.xenondevs.nova.resources.builder

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.commons.reflection.simpleNestedName
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.resources.ResourcePackManager
import xyz.xenondevs.nova.resources.builder.task.BuildStage
import xyz.xenondevs.nova.resources.builder.task.PackBuildData
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.util.ForwardingLogger
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private class PackTaskCreator<D : PackBuildData>(
    val dataType: KClass<out D>?,
    val ctor: D.(ResourcePackBuilder) -> PackTask
) {
    
    @Suppress("UNCHECKED_CAST")
    fun create(data: List<PackBuildData>, builder: ResourcePackBuilder): PackTask {
        if (dataType == null) {
            val data = (object : PackBuildData {}) as D
            return ctor(data, builder)
        }
        
        val data = data.firstOrNull { candidate ->
            candidate::class.isSubclassOf(dataType)
        } ?: throw IllegalArgumentException("Pack task requires build data of type $dataType, but it is not present")
        
        return ctor(data as D, builder)
    }
    
}

/**
 * A configuration for a custom resource pack to be generated and served by Nova.
 */
class ResourcePackConfiguration internal constructor(val id: Key) {
    
    private val dataConstructors = ArrayList<(ResourcePackBuilder) -> PackBuildData>()
    private val taskConstructors = ArrayList<PackTaskCreator<*>>()
    
    /**
     * Whether this resource pack is enabled for players by default. Defaults to `true`.
     * 
     * The pack can later be enabled or disabled per player via [ResourcePackManager].
     */
    var isEnabledByDefault = true
    
    /**
     * The logger used for the resource pack build process.
     */
    var logger = ComponentLogger.logger("Nova >> $id")
    
    /**
     * Registers a [PackBuildData] to be present during the resource pack build process.
     */
    fun registerBuildData(data: (ResourcePackBuilder) -> PackBuildData) {
        dataConstructors += data
    }
    
    /**
     * Registers a [PackBuildData] to be present during the resource pack build process.
     */
    fun registerBuildData(data: () -> PackBuildData) {
        dataConstructors += { data() }
    }
    
    /**
     * Registers a [PackTask] to be run during the resource pack build process.
     * Requires the presence of [PackBuildData] of type [D].
     */
    @JvmName("registerTask1")
    inline fun <reified D : PackBuildData> registerTask(noinline task: D.() -> PackTask) {
        val task1: D.(ResourcePackBuilder) -> PackTask = { task() }
        registerTask(task1)
    }
    
    /**
     * Registers a [PackTask] to be run during the resource pack build process.
     * Requires the presence of [PackBuildData] of type [D].
     */
    @JvmName("registerTask2")
    inline fun <reified D : PackBuildData> registerTask(noinline task: D.(ResourcePackBuilder) -> PackTask) {
        registerTask(D::class, task)
    }
    
    @PublishedApi
    internal fun <D : PackBuildData> registerTask(clazz: KClass<out D>, task: D.(ResourcePackBuilder) -> PackTask) {
        taskConstructors += PackTaskCreator(clazz, task)
    }
    
    /**
     * Registers a [PackTask] to be run during the resource pack build process.
     */
    @JvmName("registerTask3")
    fun registerTask(task: (ResourcePackBuilder) -> PackTask) {
        taskConstructors += PackTaskCreator<PackBuildData>(null) { task(it) }
    }
    
    /**
     * Registers a [PackTask] to be run during the resource pack build process.
     */
    @JvmName("registerTask4")
    fun registerTask(task: () -> PackTask) {
        taskConstructors += PackTaskCreator<PackBuildData>(null) { task() }
    }
    
    internal fun create(extraListener: Audience? = null): ResourcePackBuilder {
        val logger = if (extraListener != null) ForwardingLogger(logger, extraListener) else logger
        val builder = ResourcePackBuilder(id, logger)
        
        val data = dataConstructors.map { it(builder) }
        val tasks = taskConstructors.map { it.create(data, builder) }
        val taskGraphs = buildTaskGraphs(tasks)
        
        builder.data = data
        builder.tasks = taskGraphs
        
        return builder
    }
    
    private fun buildTaskGraphs(unorderedTasks: List<PackTask>): Map<BuildStage, List<PackTask>> {
        val taskByClass: Map<KClass<out PackTask>, PackTask> =
            unorderedTasks.associateByTo(HashMap()) { it::class }
        
        val preWorldMarker = object : PackTask {
            override suspend fun run() = Unit
            override fun toString(): String = "__STAGE_TRANSITION_MARKER__"
        }
        
        val graph = DirectedAcyclicGraph<PackTask, DefaultEdge>(DefaultEdge::class.java)
        
        graph.addVertex(preWorldMarker)
        
        for (task in unorderedTasks) {
            graph.addVertex(task)
            when (task.stage) {
                BuildStage.PRE_WORLD -> graph.addEdge(task, preWorldMarker)
                BuildStage.POST_WORLD -> graph.addEdge(preWorldMarker, task)
                BuildStage.AUTOMATIC -> Unit
            }
        }
        
        for (task in unorderedTasks) {
            task.runAfter
                .mapNotNull { taskByClass[it] }
                .forEach { runTaskAfterThis -> graph.addEdge(runTaskAfterThis, task) }
            task.runBefore
                .mapNotNull { taskByClass[it] }
                .forEach { runTaskBeforeThis -> graph.addEdge(task, runTaskBeforeThis) }
        }
        
        if (IS_DEV_SERVER)
            dumpGraph(graph)
        
        graph.iterator()
        val orderedTasks = graph.toList()
        val i = orderedTasks.indexOf(preWorldMarker)
        return enumMapOf(
            BuildStage.PRE_WORLD to orderedTasks.subList(0, i),
            BuildStage.POST_WORLD to orderedTasks.subList(i + 1, orderedTasks.size)
        )
    }
    
    private fun dumpGraph(graph: Graph<PackTask, DefaultEdge>) {
        val sanitizedId = id.toString().replace(Regex("[:/]"), "_")
        
        val file = File("debug/nova/resource_pack_$sanitizedId.dot")
        file.parentFile.mkdirs()
        
        val exporter = DOTExporter<PackTask, DefaultEdge>()
        exporter.setVertexAttributeProvider { vertex ->
            mapOf("label" to DefaultAttribute.createAttribute(vertex::class.simpleNestedName ?: vertex.toString()))
        }
        exporter.exportGraph(graph, file)
    }
    
}