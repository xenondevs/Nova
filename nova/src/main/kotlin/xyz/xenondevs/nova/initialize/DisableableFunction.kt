package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import xyz.xenondevs.nova.annotations.AnnotationIndex
import org.jgrapht.Graph

internal class DisableableFunction(
    private val classLoader: ClassLoader,
    private val className: String,
    private val method: AnnotationIndex.Method,
    override val dispatcher: CoroutineDispatcher?,
    private val runBeforeNames: Set<String>,
    private val runAfterNames: Set<String>
) : InitializerRunnable<DisableableFunction>() {
    
    override fun loadDependencies(all: Set<DisableableFunction>, graph: Graph<DisableableFunction, *>) {
        // this runBefore that
        runBeforeNames
            .flatMap { runBeforeName -> all.filter { it.className == runBeforeName } }
            .forEach { graph.addEdge(this, it) }
        
        // this runAfter that
        runAfterNames
            .flatMap { runAfterName -> all.filter { it.className == runAfterName } }
            .forEach { graph.addEdge(it, this) }
    }
    
    override suspend fun run() {
        val clazz = Class.forName(className.replace('/', '.'), true, classLoader)
        callMethod(clazz, method)
        completion.complete(Unit)
    }
    
    override fun toString(): String {
        return "${className}::${method.name}"
    }
    
    companion object {
        
        fun fromInitAnnotation(
            classLoader: ClassLoader,
            className: String, method: AnnotationIndex.Method,
            annotation: AnnotationIndex.Annotation
        ) = DisableableFunction(
            classLoader,
            className, method,
            (readDispatcher(annotation) ?: Dispatcher.SYNC).dispatcher,
            readStrings("runBefore", annotation),
            readStrings("runAfter", annotation)
        )
        
    }
    
}