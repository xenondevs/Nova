package xyz.xenondevs.nova.initialize

import xyz.xenondevs.commons.reflection.hasEmptyArguments
import xyz.xenondevs.nova.LOGGER
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible

internal open class InitializableClass(
    val classLoader: ClassLoader,
    val className: String,
    val dependsOn: Set<String>
) {
    
    private val clazz by lazy { Class.forName(className.replace('/', '.'), true, classLoader).kotlin }
    internal val initialization = CompletableFuture<Boolean>()
    
    internal fun initialize() {
        try {
            // load class, call init method(s)
            clazz.declaredFunctions.asSequence()
                .filter { it.hasAnnotation<InitFun>() && it.hasEmptyArguments() }
                .forEach {
                    val instance = clazz.objectInstance
                        ?: throw InitializationException("Initializable class $className is not a singleton")
                    
                    it.isAccessible = true
                    it.call(instance)
                }
            
            initialization.complete(true)
        } catch (e: Exception) {
            val cause = if (e is InvocationTargetException) e.targetException else e
            
            if (cause is InitializationException) {
                LOGGER.severe(cause.message)
            } else {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to initialize $this", cause)
            }
        }
        initialization.complete(false)
    }
    
    internal fun disable() {
        // call disable method(s)
        clazz.declaredFunctions.asSequence()
            .filter { it.hasAnnotation<DisableFun>() && it.hasEmptyArguments() }
            .forEach {
                val instance = clazz.objectInstance
                    ?: throw InitializationException("Initializable class $className is not a singleton")
                
                it.isAccessible = true
                it.call(instance)
            }
    }
    
    override fun toString() = className
    
}