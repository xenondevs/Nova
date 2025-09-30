package xyz.xenondevs.nova.resources.builder.task

import kotlin.reflect.KClass

/**
 * A stage in the resource pack build process.
 */
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
 * Container for data that is shared between [PackTasks][PackTask] during the resource pack build process.
 */
interface PackBuildData

/**
 * An action that is performed during the resource pack build process.
 */
interface PackTask {
    
    /**
     * The stage in which this task should be run.
     * Defaults to [BuildStage.AUTOMATIC].
     */
    val stage: BuildStage
        get() = BuildStage.AUTOMATIC
    
    /**
     * A set of tasks that this task depends on.
     * This task runs after these tasks.
     */
    val runsAfter: Set<KClass<out PackTask>>
        get() = emptySet()
    
    /**
     * A set of tasks that depend on this task.
     * This task runs before these tasks.
     */
    val runsBefore: Set<KClass<out PackTask>>
        get() = emptySet()
    
    /**
     * Runs this task.
     */
    suspend fun run()
    
}