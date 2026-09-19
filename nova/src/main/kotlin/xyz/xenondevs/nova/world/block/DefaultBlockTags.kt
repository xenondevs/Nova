package xyz.xenondevs.nova.world.block

import xyz.xenondevs.nova.registry.MinecraftRegistrar
import xyz.xenondevs.nova.registry.NovaRegistrar
import xyz.xenondevs.nova.registry.tags.BlockTypeTags

object DefaultBlockTags {
    
    /**
     * Contains all Nova blocks.
     */
    val NOVA = NovaRegistrar.blockTag("nova") {}
    
    init {
        val needsCopperTool = MinecraftRegistrar.blockTag("minecraft:needs_copper_tool") {}
        val needsNetheriteTool = MinecraftRegistrar.blockTag("minecraft:needs_netherite_tool") {}
        
        MinecraftRegistrar.tag(BlockTypeTags.NEEDS_STONE_TOOL) {
            add(needsCopperTool)
        }
        
        MinecraftRegistrar.tag(BlockTypeTags.NEEDS_DIAMOND_TOOL) {
            add(needsNetheriteTool)
        }
        
        MinecraftRegistrar.tag(BlockTypeTags.INCORRECT_FOR_DIAMOND_TOOL) {
            add(needsNetheriteTool)
        }
    }
    
}