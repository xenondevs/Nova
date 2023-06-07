package xyz.xenondevs.nova.initialize

import org.objectweb.asm.Type
import xyz.xenondevs.commons.reflection.hasEmptyArguments
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.Nova
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible

internal class InitializableClass(
    val classLoader: ClassLoader,
    val className: String,
    val stage: InternalInitStage,
    private val runBeforeNames: Set<String>,
    private val runAfterNames: Set<String>
) {
    
    private val clazz by lazy { Class.forName(className.replace('/', '.'), true, classLoader).kotlin }
    val dependsOn = HashSet<InitializableClass>()
    val initialization = CompletableFuture<Boolean>()
    
    init {
        initialization.thenAccept { success ->
            if (success) Initializer.initialized += this
        }
    }
    
    fun loadDependencies(classes: List<InitializableClass>) {
        // this class is initialized before those classes
        val runBefore = runBeforeNames.map { runBeforeName ->
            val runBefore = classes.firstOrNull { candidate -> candidate.className == runBeforeName }
                ?: throw IllegalStateException("Could not find initializable class '$runBeforeName', which is a runBefore of '$className'")
            
            if (!stage.isPreWorld && runBefore.stage.isPreWorld)
                throw IllegalStateException("Incompatible stages: '$className' (post-world) is configured to be initialized before '$runBeforeName' (pre-world)")
            
            if (runBefore.initialization.isDone)
                throw IllegalStateException("'$className' is configured to be initialized before '$runBeforeName', but '$runBeforeName' is already initialized")
                
            return@map runBefore
        }
        runBefore.forEach { it.dependsOn += this }
        
        // this class is initialized after those classes
        val runAfter = runAfterNames.map { runAfterName ->
            val runAfter = classes.firstOrNull { candidate -> candidate.className == runAfterName }
                ?: throw IllegalStateException("Could not find initializable class '$runAfterName', which is a runAfter of '$className'")
            
            if (stage.isPreWorld && !runAfter.stage.isPreWorld)
                throw IllegalStateException("Incompatible stages: '$className' (pre-world) is configured to be initialized after '$runAfterName' (post-world)")
            
            return@map runAfter
        }
        dependsOn += runAfter
    }
    
    fun initialize(): Boolean {
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
            return true
        } catch (e: Exception) {
            val cause = if (e is InvocationTargetException) e.targetException else e
            
            if (cause is InitializationException) {
                LOGGER.severe(cause.message)
            } else {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to initialize $this", cause)
            }
            
            initialization.complete(false)
            return false
        }
    }
    
    fun disable() {
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
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        fun fromAddonAnnotation(classLoader: ClassLoader, clazz: String, annotation: Map<String, Any?>): InitializableClass {
            val stageName = (annotation["stage"] as Array<String>?)?.get(1)
                ?: throw IllegalStateException("Init annotation on $clazz does not contain a stage!")
            val stage = enumValueOf<InitStage>(stageName)
            
            val runBefore = (annotation["runBefore"] as List<Type>?)?.mapTo(HashSet()) { it.internalName } ?: HashSet()
            runBefore += stage.runBefore
            
            val runAfter = (annotation["runAfter"] as List<Type>?)?.mapTo(HashSet()) { it.internalName } ?: HashSet()
            runAfter += stage.runAfter
            
            return InitializableClass(classLoader, clazz, stage.internalStage, runBefore, runAfter)
        }
        
        // Map structure: https://i.imgur.com/VHLkAtM.png (stage instead of initializationStage)
        fun fromInternalAnnotation(clazz: String, annotation: Map<String, Any?>): InitializableClass {
            val stageName = (annotation["stage"] as Array<String>?)?.get(1)
                ?: throw IllegalStateException("InternalInit annotation on $clazz does not contain a stage!")
            val stage = enumValueOf<InternalInitStage>(stageName)
            val dependsOn = (annotation["dependsOn"] as List<Type>?)?.mapTo(HashSet()) { it.internalName } ?: emptySet()
            
            return InitializableClass(Nova::class.java.classLoader, clazz, stage, emptySet(), dependsOn)
        }
        
    }
    
}