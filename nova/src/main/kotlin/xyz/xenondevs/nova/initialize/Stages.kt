package xyz.xenondevs.nova.initialize

import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.resources.ResourceGeneration
import kotlin.reflect.KClass

/**
 * Defines the stage at which something should be initialized.
 */
internal enum class InternalInitStage(val isPreWorld: Boolean) {
    
    /**
     * Before the world is loaded.
     */
    PRE_WORLD(true),
    
    /**
     * After the world is loaded.
     */
    POST_WORLD(false),
    
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
     * Before configs are initialized.
     * Can be used to register custom config serializers via [Configs.registerSerializers].
     */
    PRE_CONFIG(InternalInitStage.PRE_WORLD, runBefore = setOf(Configs::class)),
    
    /**
     * Before the world is loaded.
     */
    PRE_WORLD(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class)),
    
    /**
     * Before the resource pack generation starts.
     */
    PRE_PACK(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class), runBefore = setOf(ResourceGeneration.PreWorld::class)),
    
    /**
     * After the first stage of resource pack generation ("pre-world") has finished. Lookup registries are now loaded.
     */
    POST_PACK_PRE_WORLD(InternalInitStage.PRE_WORLD, runAfter = setOf(Configs::class, ResourceGeneration.PreWorld::class)),
    
    /**
     * After the world has been loaded.
     */
    POST_WORLD(InternalInitStage.POST_WORLD),
    
    /**
     * After the second (and last) stage of resource pack generation ("post-world") has finished.
     */
    POST_PACK(InternalInitStage.POST_WORLD, runAfter = setOf(ResourceGeneration.PostWorld::class));
    
    internal val runAfter: Array<String> = runAfter.mapToArray { it.internalName }
    internal val runBefore: Array<String> = runBefore.mapToArray { it.internalName }
    
}