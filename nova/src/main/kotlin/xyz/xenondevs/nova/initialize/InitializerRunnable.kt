package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import org.jgrapht.Graph
import org.objectweb.asm.Type
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.AnnotationValue
import java.lang.invoke.MethodType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

internal sealed class InitializerRunnable<S : InitializerRunnable<S>> {
    
    val completion = CompletableDeferred<Unit>()
    abstract val dispatcher: CoroutineDispatcher?
    
    abstract fun loadDependencies(all: Set<S>, graph: Graph<S, *>)
    
    abstract suspend fun run()
    
    protected suspend fun callMethod(clazz: Class<*>, method: AnnotationIndex.Method) {
        val methodType = MethodType.fromMethodDescriptorString(method.descriptor, clazz.classLoader)
        val function = clazz.getDeclaredMethod(method.name, *methodType.parameterArray()).kotlinFunction!!
        function.isAccessible = true
        function.callSuspend(clazz.kotlin.objectInstance)
    }
    
    companion object {
        
        fun readStrings(name: String, annotation: AnnotationIndex.Annotation): HashSet<String> {
            return (annotation.arguments[name] as AnnotationValue.ArrayValue?)?.values
                ?.mapTo(HashSet()) { Type.getType((it as AnnotationValue.ClassLiteral).descriptor).internalName }
                ?: HashSet()
        }
        
        fun readDispatcher(annotation: AnnotationIndex.Annotation): Dispatcher? {
            return (annotation.arguments["dispatcher"] as AnnotationValue.EnumConstant?)?.name
                ?.let { enumValueOf<Dispatcher>(it) }
        }
        
        fun readAnnotationCommons(annotation: AnnotationIndex.Annotation): Triple<Dispatcher?, HashSet<String>, HashSet<String>> {
            val dispatcher = readDispatcher(annotation)
            val runBefore = readStrings("runBefore", annotation)
            val runAfter = readStrings("runAfter", annotation)
            return Triple(dispatcher, runBefore, runAfter)
        }
        
    }
    
}