package xyz.xenondevs.nova.resources.builder.task

import kotlin.reflect.KClass

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


interface PackBuildData

interface PackTask {
    
    val stage: BuildStage
        get() = BuildStage.AUTOMATIC
    val runBefore: Set<KClass<out PackTask>>
        get() = emptySet()
    val runAfter: Set<KClass<out PackTask>>
        get() = emptySet()
    
    suspend fun run()
    
}