package xyz.xenondevs.nova.world.item.tool

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object VanillaToolTiers {
    
    val WOOD = register("wood")
    val GOLD = register("gold")
    val STONE = register("stone")
    val COPPER = register("copper")
    val IRON = register("iron")
    val DIAMOND = register("diamond")
    val NETHERITE = register("netherite")
    
    private fun register(name: String): ToolTier {
        val id = Key.key(name)
        val level = ToolTier(id, Configs["nova:tool_levels"].entry(id.value()))
        
        NovaRegistries.TOOL_TIER[id] = level
        return level
    }
    
}