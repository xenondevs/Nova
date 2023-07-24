package xyz.xenondevs.nova.initialize

import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.transformer.Patcher
import kotlin.reflect.KClass

/**
 * Defines the stage at which something should be initialized.
 */
internal enum class InternalInitStage(val isPreWorld: Boolean) {
    
    /**
     * Before the world is loaded, in the server thread.
     */
    PRE_WORLD(true),
    
    /**
     * After the world is loaded, in the server thread.
     */
    POST_WORLD(false),
    
    /**
     * After the world is loaded, in an async thread.
     */
    POST_WORLD_ASYNC(false);
    
}

/**
 * Defines the stage at which something should be initialized.
 *
 * @param internalStage The internal [InternalInitStage] to use.
 * @param runAfter The default set of initializables that should this one should be initialized after.
 * @param runBefore The default set of initializables that should this one should be initialized before.
 */
enum class InitStage(
    internal val internalStage: InternalInitStage,
    runAfter: Set<KClass<*>> = emptySet(),
    runBefore: Set<KClass<*>> = emptySet(),
) {
    
    /**
     * Before the world is loaded.
     */
    PRE_WORLD(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class, Patcher::class)),
    
    /**
     * Before the resource pack generation starts.
     */
    PRE_PACK(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class, Patcher::class), runBefore = setOf(ResourceGeneration.PreWorld::class)),
    
    /**
     * After the first stage of resource pack generation ("pre-world") has finished. Lookup registries are now loaded.
     */
    POST_PACK_PRE_WORLD(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class, Patcher::class, ResourceGeneration.PreWorld::class)),
    
    /**
     * After the world has been loaded.
     */
    POST_WORLD(InternalInitStage.POST_WORLD),
    
    /**
     * After the world has been loaded, in an async thread.
     */
    POST_WORLD_ASYNC(InternalInitStage.POST_WORLD_ASYNC),
    
    /**
     * After the second (and last) stage of resource pack generation ("post-world") has finished.
     */
    POST_PACK(InternalInitStage.POST_WORLD, runAfter = setOf(ResourceGeneration.PostWorld::class)),
    
    /**
     * After the second (and last) stage of resource pack generation ("post-world") has finished, in an async thread.
     */
    POST_PACK_ASYNC(InternalInitStage.POST_WORLD_ASYNC, runAfter = setOf(ResourceGeneration.PostWorld::class));
    
    internal val runAfter: Array<String> = runAfter.mapToArray { it.internalName }
    internal val runBefore: Array<String> = runBefore.mapToArray { it.internalName }
    
}