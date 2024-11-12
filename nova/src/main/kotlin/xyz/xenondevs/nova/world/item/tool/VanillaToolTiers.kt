package xyz.xenondevs.nova.world.item.tool

import net.minecraft.resources.ResourceLocation
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
    val IRON = register("iron")
    val DIAMOND = register("diamond")
    val NETHERITE = register("netherite")
    
    private fun register(name: String): ToolTier {
        val id = ResourceLocation.withDefaultNamespace(name)
        val level = ToolTier(id, Configs["nova:tool_levels"].entry(id.path))
        
        NovaRegistries.TOOL_TIER[id] = level
        return level
    }
    
}