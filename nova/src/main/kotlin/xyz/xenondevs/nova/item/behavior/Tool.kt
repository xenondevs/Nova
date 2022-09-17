package xyz.xenondevs.nova.item.behavior

import org.bukkit.block.Block
import xyz.xenondevs.nova.material.options.ToolOptions

abstract class Tool(val options: ToolOptions) : ItemBehavior() {
    
    abstract fun checkCorrectTool(block: Block): Boolean
    
}