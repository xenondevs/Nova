package xyz.xenondevs.nova.data.resources.builder.task

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.resources.builder.task.font.FontContent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.jvm.isAccessible

/**
 * The build stages during the resource pack build process. The enums are ordered by their execution order.
 */
enum class BuildStage {
    
    /**
     * Before anything has been written to the build directory
     */
    INIT,
    
    /**
     * After base pack assets have been copied to the build directory
     */
    POST_BASE_PACKS,
    
    /**
     * The stage during which assets should be extracted from asset packs to the build directory.
     */
    EXTRACT_ASSETS,
    
    /**
     * After all assets have been copied to the build directory.
     * [PackTasks][PackTask] should use this stage to read and handle extracted assets.
     */
    POST_EXTRACT_ASSETS,
    
    /**
     * The pre-world writing stage.
     * [PackTasks][PackTask] should use this stage to generate assets and data that is required for post-world operations.
     */
    PRE_WORLD_WRITE,
    
    /**
     * The post-world writing stage.
     *
     * [PackTasks][PackTask] should use this stage to write all assets that require the world to be loaded or
     * need to access hooks such as custom item services.
     */
    POST_WORLD_WRITE,
    
    /**
     * Called immediately after the [POST_WORLD_WRITE].
     *
     * This stage is intended to be used by [PackTasks][PackTask] that provide an interface for other [PackTasks][PackTask],
     * such as the [FontContent] task. If more stages are added in the future, this stage is intended to always stay the
     * last writing stage.
     */
    LATE_WRITE,
    
    /**
     * After post-world writing, should only be used to analyze generated assets (for example to calculate char sizes).
     * 
     * Writing is not allowed during this stage.
     */
    ANALYZE
    
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
    val stage: BuildStage,
    val runAfter: Array<KClass<*>> = [],
    val runBefore: Array<KClass<*>> = []
)

internal class PackFunction(
    private val holder: PackTaskHolder,
    private val clazz: KClass<*>,
    private val func: KFunction<*>,
    private val runAfterClasses: Set<KClass<*>>,
    private val runBeforeClasses: Set<KClass<*>>
) {
    
    lateinit var runAfter: Set<PackFunction>
    lateinit var runBefore: Set<PackFunction>
    
    private fun loadDependencies(functions: List<PackFunction>) {
        runAfter = functions.filterTo(HashSet()) { it.clazz in runAfterClasses }
        runBefore = functions.filterTo(HashSet()) { it.clazz in runBeforeClasses }
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
         * Extracts [PackFunctions][PackFunction] from the given [holders], groups them by their [BuildStage]
         * and sorts them based on [PackTask.runAfter] and [PackTask.runBefore].
         * 
         * It is guaranteed that all [BuildStages][BuildStage] will be present in the returned map,
         * regardless of whether there are any [PackFunctions][PackFunction] for that stage.
         *
         * @throws IllegalArgumentException If a circular dependency is detected.
         */
        fun sortGrouped(holders: Collection<PackTaskHolder>): Map<BuildStage, List<PackFunction>> {
            val map = enumMap<BuildStage, MutableList<PackFunction>>()
            
            // load all pack functions
            for (holder in holders) {
                val holderClass = holder::class
                for (func in holder::class.java.kotlin.declaredFunctions) {
                    val annotation = func.findAnnotations<PackTask>().firstOrNull() ?: continue
                    map.getOrPut(annotation.stage, ::ArrayList) += PackFunction(
                        holder, holderClass, func,
                        annotation.runAfter.toHashSet(),
                        annotation.runBefore.toHashSet()
                    )
                }
            }
            
            // sort pack functions
            for (stage in BuildStage.values()) {
                // make sure that all stages are included in th map
                if (stage !in map) {
                    map[stage] = mutableListOf()
                    continue
                }
                
                val functions = map[stage]!!
                for (func in functions) {
                    func.loadDependencies(functions)
                }
                
                map[stage] = CollectionUtils.sortDependencies(functions, PackFunction::runAfter, PackFunction::runBefore).toMutableList()
            }
            
            return map
        }
        
    }

}