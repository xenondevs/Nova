package xyz.xenondevs.nova.data.resources.builder.content

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder

enum class BuildingStage {
    PRE_WORLD,
    POST_WORLD
}

interface PackContent {
    
    /**
     * Initializes this [PackContent].
     */
    fun init() = Unit
    
    /**
     * Checks if the given [path] is excluded due to this pack content.
     */
    fun excludesPath(path: ResourcePath): Boolean = false
    
    /**
     * Writes all data related to the [pack] in the build dir.
     */
    fun includePack(pack: AssetPack) = Unit
    
    /**
     * Writes remaining data to the build dir.
     *
     * This function is run after [includePack] has been called for all asset packs.
     */
    fun write() = Unit
    
}

sealed interface PackTask {
    
    /**
     * The resource pack building stage at which [PackContent.includePack] and [PackContent.write] will be called.
     *
     * If this property is null, the stage will be determined based on [runBefore] and [runAfter].
     * If there are none, this [PackTask] will run at [BuildingStage.PRE_WORLD].
     */
    val stage: BuildingStage?
        get() = null
    
    /**
     * A set of [PackContentTypes][PackContentType] that should run after this [PackContentType].
     */
    val runBefore: Set<PackTask>
        get() = emptySet()
    
    /**
     * A set of [PackContentTypes][PackContentType] that should run before this [PackContentType].
     */
    val runAfter: Set<PackTask>
        get() = emptySet()
    
    companion object {
        
        /**
         * Sorts the given [tasks] by their [runBefore] and [runAfter] properties and groups them by their [stage].
         *
         * @throws IllegalArgumentException If a circular dependency is detected.
         */
        fun sortGrouped(tasks: Collection<PackTask>): Map<BuildingStage, List<PackTask>> {
            val map = enumMap<BuildingStage, ArrayList<PackTask>>()
            for (task in tasks) {
                var stage = task.stage
                if (stage == null) {
                    if (// this task runs after a post-world task
                        task.runAfter.any { it.stage == BuildingStage.POST_WORLD }
                        // another task is post-world and needs to run before this task
                        || tasks.any { it.stage == BuildingStage.POST_WORLD && task in it.runBefore }
                    ) {
                        stage = BuildingStage.POST_WORLD
                    } else {
                        stage = BuildingStage.PRE_WORLD
                    }
                }
                
                map.getOrPut(stage, ::ArrayList) += task
            }
            return map
        }
        
        /**
         * Sorts the given [tasks] by their [runBefore] and [runAfter] properties.
         *
         * @throws IllegalArgumentException If the given [tasks] are not all in the same [stage] or if a circular dependency is detected.
         */
        fun sort(tasks: Collection<PackTask>): List<PackTask> {
            if (tasks.isEmpty())
                return emptyList()
            require(tasks.all { it.stage == tasks.first().stage }) { "All tasks must be in the same stage" }
            return CollectionUtils.sortDependencies(tasks, PackTask::runAfter, PackTask::runBefore)
        }
        
    }
    
}

interface PackContentType<T : PackContent> : PackTask {
    
    /**
     * Creates a new [PackContent] instance of this type.
     */
    fun create(builder: ResourcePackBuilder): T
    
}

interface PackAnalyzer : PackTask {
    
    fun analyze(builder: ResourcePackBuilder)
    
}