package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.world.block.sound.SoundGroup

interface Breakable {
    
    val hardness: Double
    val toolCategories: List<ToolCategory>
    val toolTier: ToolTier?
    val requiresToolForDrops: Boolean
    val soundGroup: SoundGroup
    
}