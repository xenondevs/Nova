package xyz.xenondevs.nova.data.resources.builder.task

import xyz.xenondevs.commons.collections.CollectionUtils
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.jvm.isAccessible

enum class BuildStage {
    
    /**
     * Pack tasks with this stage are automatically assigned a stage based on their dependencies.
     */
    AUTOMATIC,
    
    /**
     * Pack tasks with this stage will always run before the world is loaded.
     */
    PRE_WORLD,
    
    /**
     * Pack tasks with this stage will always run after the world is loaded.
     */
    POST_WORLD
    
}

/**
 * An interface used to mark classes that contain [PackTask] functions.
 */
interface PackTaskHolder

/** An annotation to mark [PackTask] functions.
 *
 * @param stage Determines at which [BuildStage] this function will be run.
 * @param runAfter The [PackTasks][PackTask] which should be run before this class.
 * This only affects functions that are configured to also run at the same [BuildStage].
 * @param runBefore The classes which should be initialized after this class.
 * This only affects functions that are configured to also run at the same [BuildStage].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PackTask(
    val stage: BuildStage = BuildStage.AUTOMATIC,
    val runAfter: Array<String> = [],
    val runBefore: Array<String> = []
)

internal class PackFunction(
    private val holder: PackTaskHolder,
    private val clazz: KClass<*>,
    private val func: KFunction<*>,
    stage: BuildStage,
    private val runAfterNames: Set<String>,
    private val runBeforeNames: Set<String>
) {
    
    private var stageSource: PackFunction? = if (stage != BuildStage.AUTOMATIC) this else null
    var stage: BuildStage = stage
        private set
    
    lateinit var runAfter: Set<PackFunction>
    lateinit var runBefore: Set<PackFunction>
    
    private fun loadDependencies(functions: List<PackFunction>) {
        runAfter = runAfterNames.mapTo(HashSet()) { name ->
            val thisAfter = functions.firstOrNull { func -> func.toString() == name }
                ?: throw IllegalStateException("Could not find pack function $func, which is a runAfter of $this")
            
            val thisAfterStage = thisAfter.stage
            when {
                stage == BuildStage.PRE_WORLD && thisAfterStage == BuildStage.POST_WORLD ->
                    throw IllegalStateException("Incompatible stages: Pack function $this, which inherited its pre-world stage from $stageSource is configured to run after $thisAfter (post-world)")
                
                stage == BuildStage.AUTOMATIC && thisAfterStage == BuildStage.POST_WORLD -> {
                    stage = BuildStage.POST_WORLD
                    stageSource = thisAfter
                }
                
                stage == BuildStage.PRE_WORLD && thisAfterStage == BuildStage.AUTOMATIC -> {
                    thisAfter.stage = BuildStage.PRE_WORLD
                    thisAfter.stageSource = this
                }
            }
            
            return@mapTo thisAfter
        }
        
        runBefore = runBeforeNames.mapTo(HashSet()) { name ->
            val thisBefore = functions.firstOrNull { func -> func.toString() == name }
                ?: throw IllegalStateException("Could not find pack function $func, which is a runBefore of $this")
            val thisBeforeStage = thisBefore.stage
            
            when {
                stage == BuildStage.POST_WORLD && thisBeforeStage == BuildStage.PRE_WORLD ->
                    throw IllegalStateException("Incompatible stages: Pack function $this, which inherited its post-world stage from $stageSource is configured to run before $thisBefore (pre-world)")
                
                stage == BuildStage.AUTOMATIC && thisBeforeStage == BuildStage.PRE_WORLD -> {
                    stage = BuildStage.PRE_WORLD
                    stageSource = thisBefore
                }
                
                stage == BuildStage.POST_WORLD && thisBeforeStage == BuildStage.AUTOMATIC -> {
                    thisBefore.stage = BuildStage.POST_WORLD
                    thisBefore.stageSource = this
                }
            }
            
            return@mapTo thisBefore
        }
    }
    
    init {
        func.isAccessible = true
    }
    
    fun run() {
        func.call(holder)
    }
    
    override fun toString(): String = "${clazz.simpleName}#${func.name}"
    
    companion object {
        
        /**
         * Extracts [PackFunctions][PackFunction] from the given [holders] and sorts them based on [PackTask.runAfter] and [PackTask.runBefore].
         *
         * @throws IllegalArgumentException If a circular dependency is detected.
         */
        fun getAndSortFunctions(holders: Collection<PackTaskHolder>): List<PackFunction> {
            val functions = ArrayList<PackFunction>()
            
            // load all pack functions
            for (holder in holders) {
                val holderClass = holder::class
                for (func in holder::class.java.kotlin.declaredFunctions) {
                    val annotation = func.findAnnotations<PackTask>().firstOrNull() ?: continue
                    functions += PackFunction(
                        holder, holderClass, func,
                        annotation.stage,
                        annotation.runAfter.toHashSet(),
                        annotation.runBefore.toHashSet()
                    )
                }
            }
            
            functions.forEach { it.loadDependencies(functions) }
            functions.forEach { if (it.stage == BuildStage.AUTOMATIC) it.stage = BuildStage.PRE_WORLD }
            
            return CollectionUtils.sortDependencies(functions, PackFunction::runAfter, PackFunction::runBefore)
        }
        
    }
    
}