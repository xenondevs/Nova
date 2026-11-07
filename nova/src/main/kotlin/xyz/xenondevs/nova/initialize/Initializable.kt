package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import org.jgrapht.Graph
import org.objectweb.asm.Type
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.AnnotationValue

internal class Initializable private constructor(
    private val classLoader: ClassLoader,
    private val className: String,
    private val method: AnnotationIndex.Method?,
    val stage: InternalInitStage,
    override val dispatcher: CoroutineDispatcher?,
    private val runBeforeNames: Set<String>,
    private val runAfterNames: Set<String>
) : InitializerRunnable<Initializable>() {
    
    override fun loadDependencies(all: Set<Initializable>, graph: Graph<Initializable, *>) {
        // A function always waits for its containing class's explicit initialization task.
        if (method != null) {
            all.firstOrNull { it.className == className && it.method == null }
                ?.let { addDependency(graph, it, this) }
        }
        
        for (name in runBeforeNames) {
            val targets = findTargets(all, name)
            // Waiting for class initialization also orders all of that class's functions.
            val clazz = targets.firstOrNull { it.method == null }
            for (target in if (clazz != null) listOf(clazz) else targets) {
                addDependency(graph, this, target)
            }
        }
        
        for (name in runAfterNames) {
            for (target in findTargets(all, name)) {
                addDependency(graph, target, this)
            }
        }
    }
    
    private fun findTargets(all: Set<Initializable>, name: String): List<Initializable> {
        val targets = all.filter { it.className == name }
        require(targets.isNotEmpty()) { "Could not find initialization tasks for '$name', referenced by '$this'" }
        return targets
    }
    
    private fun addDependency(graph: Graph<Initializable, *>, before: Initializable, after: Initializable) {
        require(before.stage <= after.stage) {
            "Incompatible stages: '$before' (${before.stage}) is configured to be initialized before '$after' (${after.stage})"
        }
        if (before.stage != after.stage)
            return
        
        try {
            graph.addEdge(before, after)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Failed to add initialization dependency from '$before' to '$after'", e)
        }
    }
    
    override suspend fun run() {
        val clazz = Class.forName(className.replace('/', '.'), true, classLoader)
        if (method != null)
            callMethod(clazz, method)
        completion.complete(Unit)
    }
    
    override fun toString(): String {
        return if (method == null) className else "$className::${method.name}"
    }
    
    companion object {
        
        fun fromLegacyAnnotation(
            clazz: Initializable,
            method: AnnotationIndex.Method,
            annotation: AnnotationIndex.Annotation
        ): Initializable {
            val [dispatcher, runBefore, runAfter] = readAnnotationCommons(annotation)
            // The containing class dependency is added separately, without waiting for sibling functions.
            runAfter -= clazz.className
            return Initializable(
                clazz.classLoader, clazz.className, method,
                clazz.stage, dispatcher?.dispatcher ?: clazz.dispatcher,
                runBefore, runAfter
            )
        }
        
        fun fromAnnotation(
            classLoader: ClassLoader,
            className: String,
            method: AnnotationIndex.Method?,
            annotation: AnnotationIndex.Annotation
        ): Initializable {
            val [dispatcher, runBefore, runAfter] = readAnnotationCommons(annotation)
            val stageName = (annotation.arguments["stage"] as AnnotationValue.EnumConstant?)?.name
            val stage = if (annotation.descriptor == Type.getDescriptor(Init::class.java)) {
                val addonStage = stageName?.let { enumValueOf<InitStage>(it) } ?: InitStage.BOOTSTRAP
                runBefore += addonStage.runBefore
                runAfter += addonStage.runAfter
                addonStage.internalStage
            } else {
                require(annotation.descriptor == Type.getDescriptor(InternalInit::class.java))
                requireNotNull(stageName) { "InternalInit on $className does not contain a stage!" }
                enumValueOf<InternalInitStage>(stageName)
            }
            
            return Initializable(
                classLoader, className, method,
                stage, (dispatcher ?: Dispatcher.SYNC).dispatcher,
                runBefore, runAfter
            )
        }
        
    }
    
}