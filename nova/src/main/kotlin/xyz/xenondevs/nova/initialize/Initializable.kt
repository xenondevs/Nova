package xyz.xenondevs.nova.initialize

import kotlinx.coroutines.CoroutineDispatcher
import org.jgrapht.Graph
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.AnnotationValue

internal abstract class Initializable(
    val stage: InternalInitStage,
    override val dispatcher: CoroutineDispatcher?,
    private val runBeforeNames: Set<String>,
    private val runAfterNames: Set<String>
) : InitializerRunnable<Initializable>() {
    
    abstract val initClass: InitializableClass
    
    override fun loadDependencies(all: Set<Initializable>, graph: Graph<Initializable, *>) {
        // this runBefore that
        for (runBeforeName in runBeforeNames) {
            val runBefore = all
                .filterIsInstance<InitializableClass>()
                .firstOrNull { candidate -> candidate.initClass.className == runBeforeName }
                ?: throw IllegalArgumentException("Could not find initializable class '$runBeforeName', which is a runBefore of '$this'")
            if (!stage.isPreWorld && runBefore.stage.isPreWorld)
                throw IllegalArgumentException("Incompatible stages: '$this' (post-world) is configured to be initialized before '$runBeforeName' (pre-world)")
            
            if (runBefore.completion.isCompleted)
                throw IllegalArgumentException("'$this' is configured to be initialized before '$runBeforeName', but '$runBeforeName' is already initialized")
            
            // stages are compatible, and execution order is already specified through those 
            if (stage != runBefore.stage)
                continue
            
            try {
                graph.addEdge(this, runBefore)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Failed to add edge from '$this' to '$runBeforeName'", e)
            }
        }
        
        // this runAfter that
        for (runAfterName in runAfterNames) {
            val runAfters = HashSet<Initializable>()
            val runAfterClass = all
                .filterIsInstance<InitializableClass>()
                .firstOrNull { candidate -> candidate.initClass.className == runAfterName }
                ?: throw IllegalArgumentException("Could not find initializable class '$runAfterName', which is a runAfter of '$this'")
            runAfters += runAfterClass
            if (runAfterClass != initClass)
                runAfters += runAfterClass.initFunctions
            
            for (runAfter in runAfters) {
                if (stage.isPreWorld && !runAfter.stage.isPreWorld)
                    throw IllegalArgumentException("Incompatible stages: '$this' (pre-world) is configured to be initialized after '$runAfterName' (post-world)")
                
                // stages are compatible, and execution order is already specified through those 
                if (stage != runAfter.stage)
                    continue
                
                try {
                    graph.addEdge(runAfter, this)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Failed to add edge from '$runAfter' to '$this'", e)
                }
            }
        }
    }
    
}

internal class InitializableClass(
    val classLoader: ClassLoader,
    val className: String,
    stage: InternalInitStage,
    dispatcher: CoroutineDispatcher?,
    runBeforeNames: Set<String>,
    runAfterNames: Set<String>
) : Initializable(stage, dispatcher, runBeforeNames, runAfterNames) {
    
    override val initClass = this
    
    lateinit var clazz: Class<*>
        private set
    
    val initFunctions = ArrayList<InitializableFunction>()
    
    override suspend fun run() {
        clazz = Class.forName(className.replace('/', '.'), true, classLoader)
        completion.complete(Unit)
    }
    
    override fun toString(): String {
        return className
    }
    
    companion object {
        
        fun fromAddonAnnotation(classLoader: ClassLoader, clazz: String, annotation: AnnotationIndex.Annotation): InitializableClass {
            val stage = (annotation.arguments["stage"] as AnnotationValue.EnumConstant?)?.name
                ?.let { enumValueOf<InitStage>(it) }
                ?: throw IllegalStateException("Init annotation on $clazz does not contain a stage!")
            val [dispatcher, runBefore, runAfter] = readAnnotationCommons(annotation)
            runBefore += stage.runBefore
            runAfter += stage.runAfter
            
            return InitializableClass(
                classLoader, clazz,
                stage.internalStage, (dispatcher ?: Dispatcher.SYNC).dispatcher,
                runBefore, runAfter
            )
        }
        
        fun fromInternalAnnotation(classLoader: ClassLoader, clazz: String, annotation: AnnotationIndex.Annotation): InitializableClass {
            val stage = (annotation.arguments["stage"] as AnnotationValue.EnumConstant?)?.name
                ?.let { enumValueOf<InternalInitStage>(it) }
                ?: throw IllegalStateException("InternalInit annotation on $clazz does not contain a stage!")
            val [dispatcher, runBefore, runAfter] = readAnnotationCommons(annotation)
            
            return InitializableClass(
                classLoader, clazz,
                stage, (dispatcher ?: Dispatcher.SYNC).dispatcher,
                runBefore, runAfter
            )
        }
        
    }
    
}

internal class InitializableFunction(
    override val initClass: InitializableClass,
    private val method: AnnotationIndex.Method,
    dispatcher: CoroutineDispatcher?,
    runBeforeNames: Set<String>,
    runAfterNames: Set<String>
) : Initializable(
    initClass.stage,
    dispatcher,
    runBeforeNames,
    runAfterNames + initClass.className
) {
    
    override suspend fun run() {
        callMethod(initClass.clazz, method)
        completion.complete(Unit)
    }
    
    override fun toString(): String {
        return initClass.className + "::" + method.name
    }
    
    companion object {
        
        fun fromInitAnnotation(clazz: InitializableClass, method: AnnotationIndex.Method, annotation: AnnotationIndex.Annotation): InitializableFunction {
            val [dispatcher, runBefore, runAfter] = readAnnotationCommons(annotation)
            val func = InitializableFunction(
                clazz, method,
                dispatcher?.dispatcher ?: clazz.dispatcher,
                runBefore, runAfter
            )
            clazz.initFunctions += func
            return func
        }
        
    }
    
}