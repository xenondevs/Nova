package xyz.xenondevs.nova.world.item.tool

import net.kyori.adventure.key.Key.key
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.RegistryEntry

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object VanillaToolTiers {
    
    val WOOD = register("wood")
    val GOLD = register("gold")
    val STONE = register("stone")
    val COPPER = register("copper")
    val IRON = register("iron")
    val DIAMOND = register("diamond")
    val NETHERITE = register("netherite")
    
    private fun register(name: String): RegistryEntry.Nova<ToolTier> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOL_TIER, key(name)) {
            ToolTier(it, CONFIGS["nova:tool_levels"].entry(name))
        }
    
}