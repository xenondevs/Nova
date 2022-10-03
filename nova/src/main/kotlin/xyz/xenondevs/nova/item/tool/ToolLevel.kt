package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolLevelRegistry.register
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos

class ToolLevel(
    val id: NamespacedId,
    levelValue: ValueReloadable<Double>
) : Comparable<ToolLevel> {
    
    internal val levelValue by levelValue
    
    override fun compareTo(other: ToolLevel): Int {
        return levelValue.compareTo(other.levelValue)
    }
    
    companion object {
        
        val STONE = register("stone")
        val IRON = register("iron")
        val DIAMOND = register("diamond")
        
        fun ofBlock(block: Block): ToolLevel? {
            val novaLevel = BlockManager.getBlock(block.pos)?.material?.toolLevel
            if (novaLevel != null)
                return novaLevel
            
            val material = block.type
            return when {
                Tag.NEEDS_STONE_TOOL.isTagged(material) -> STONE
                Tag.NEEDS_IRON_TOOL.isTagged(material) -> IRON
                Tag.NEEDS_DIAMOND_TOOL.isTagged(material) -> DIAMOND
                else -> null
            }
        }
        
        fun ofItem(item: ItemStack?): ToolLevel? {
            if (item == null)
                return null
            
            val novaLevel = item.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.level
            if (novaLevel != null)
                return novaLevel
            
            return when (item.type) {
                Material.STONE_SWORD, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE -> STONE
                
                Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE -> IRON
                
                Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE,
                Material.NETHERITE_SWORD, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE -> DIAMOND
                
                else -> null
            }
        }
        
        fun isCorrectLevel(block: Block, tool: ItemStack?): Boolean {
            val toolLevel = ofItem(tool)
            
            val novaMaterial = BlockManager.getBlock(block.pos)?.material
            if (novaMaterial != null) {
                val blockLevel = novaMaterial.toolLevel ?: return true
                return isCorrectLevel(blockLevel, toolLevel)
            }
            
            val material = block.type
            val blockLevel = when {
                Tag.NEEDS_STONE_TOOL.isTagged(material) -> STONE
                Tag.NEEDS_IRON_TOOL.isTagged(material) -> IRON
                Tag.NEEDS_DIAMOND_TOOL.isTagged(material) -> DIAMOND
                else -> null
            } ?: return true
            
            return isCorrectLevel(blockLevel, toolLevel)
        }
        
        fun isCorrectLevel(blockLevel: ToolLevel?, toolLevel: ToolLevel?): Boolean {
            return blockLevel == null || (toolLevel?.levelValue ?: 0.0) >= blockLevel.levelValue
        }
        
    }
    
}