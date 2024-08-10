package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import org.jgrapht.Graph
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

internal class DisableableFunction(
    private val classLoader: ClassLoader,
    private val className: String,
    private val methodName: String,
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
        val clazz = Class.forName(className.replace('/', '.'), true, classLoader).kotlin
        val function = clazz.functions.first {
            it.javaMethod!!.name == methodName &&
                it.parameters.size == 1 &&
                it.parameters[0].kind == KParameter.Kind.INSTANCE
        }
        function.isAccessible = true
        function.callSuspend(clazz.objectInstance)
        
        completion.complete(Unit)
    }
    
    override fun toString(): String {
        return "${className}::${methodName}"
    }
    
    companion object {
        
        fun fromInitAnnotation(
            classLoader: ClassLoader,
            className: String, methodName: String, 
            annotation: Map<String, Any?>
        ) = DisableableFunction(
            classLoader,
            className, methodName,
            (readDispatcher(annotation) ?: Dispatcher.SYNC).dispatcher,
            readStrings("runBefore", annotation),
            readStrings("runAfter", annotation)
        )
        
    }
    
}