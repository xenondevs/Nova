package xyz.xenondevs.nova.item.tool

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.name
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
        val id = ResourceLocation("minecraft", name)
        val level = ToolTier(id, configReloadable {
            NovaConfig["nova:tool_levels"].getDouble(id.name)
        })
        
        NovaRegistries.TOOL_TIER[id] = level
        return level
    }
    
}