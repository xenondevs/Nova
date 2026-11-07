package xyz.xenondevs.nova.initialize

import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.resources.ResourceGeneration
import kotlin.reflect.KClass

/**
 * Defines the stage at which something should be initialized.
 * Entries are declared in lifecycle order.
 */
internal enum class InternalInitStage {
    
    /**
     * During the server's bootstrap phase.
     */
    BOOTSTRAP,
    
    /**
     * During Nova's plugin enable, before worlds are loaded.
     */
    PRE_WORLD,
    
    /**
     * During [ServerLoadEvent].
     */
    POST_WORLD,
    
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
    
    @Deprecated(
        "Renamed to BOOTSTRAP.",
        ReplaceWith("InitStage.BOOTSTRAP", "xyz.xenondevs.nova.initialize.InitStage")
    )
    PRE_CONFIG(internalStage = InternalInitStage.BOOTSTRAP),
    
    @Deprecated(
        "Renamed to BOOTSTRAP.",
        ReplaceWith("InitStage.BOOTSTRAP", "xyz.xenondevs.nova.initialize.InitStage")
    )
    PRE_WORLD(internalStage = InternalInitStage.BOOTSTRAP),
    
    /**
     * During the server's bootstrap phase.
     */
    BOOTSTRAP(internalStage = InternalInitStage.BOOTSTRAP),
    
    /**
     * Before worlds are loaded, during Nova's [JavaPlugin.onEnable].
     */
    PRE_WORLD_LOAD(internalStage = InternalInitStage.PRE_WORLD),
    
    /**
     * Before the resource pack generation starts.
     */
    PRE_PACK(
        internalStage = InternalInitStage.BOOTSTRAP,
        runBefore = setOf(
            RegistryLoader::class,
            ResourceGeneration.PreWorld::class
        )
    ),
    
    /**
     * After the first stage of resource pack generation ("pre-world") has finished. Lookup registries are now loaded.
     */
    POST_PACK_PRE_WORLD(
        internalStage = InternalInitStage.BOOTSTRAP,
        runAfter = setOf(
            RegistryLoader::class,
            ResourceGeneration.PreWorld::class
        )
    ),
    
    /**
     * After the world has been loaded.
     */
    POST_WORLD(internalStage = InternalInitStage.POST_WORLD),
    
    /**
     * After the second (and last) stage of resource pack generation ("post-world") has finished.
     */
    POST_PACK(
        internalStage = InternalInitStage.POST_WORLD,
        runAfter = setOf(
            ResourceGeneration.PostWorld::class
        )
    );
    
    internal val runAfter: Array<String> = runAfter.mapToArray { it.internalName }
    internal val runBefore: Array<String> = runBefore.mapToArray { it.internalName }
    
}