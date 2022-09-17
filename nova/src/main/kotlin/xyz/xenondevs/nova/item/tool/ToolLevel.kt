package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolLevelRegistry.register
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import java.util.*

class ToolLevel(
    val id: NamespacedId
) {
    
    private val parentLevels = ArrayList<ToolLevel>()
    private var flatParentLevels: Set<ToolLevel> = emptySet()
    
    fun extendsFrom(vararg other: ToolLevel) {
        parentLevels += other
        makeFlatParentLevels()
    }
    
    fun clearInheritance() {
        parentLevels.clear()
        flatParentLevels = emptySet()
    }
    
    fun doesExtendFrom(other: ToolLevel): Boolean {
        return other in flatParentLevels
    }
    
    private fun makeFlatParentLevels() {
        val explored = HashSet<ToolLevel>()
        val unexplored = LinkedList<ToolLevel>().apply { addAll(parentLevels) }
        
        while (unexplored.isNotEmpty()) {
            val level = unexplored.poll()
            for (parentLevel in level.parentLevels) {
                if (parentLevel in explored)
                    throw IllegalStateException("Recursive tool level inheritance")
                
                unexplored += parentLevel
            }
            
            explored += level
        }
        
        flatParentLevels = explored
    }
    
    companion object {
        
        val STONE = register("stone")
        val IRON = register("iron").apply { extendsFrom(STONE) }
        val DIAMOND = register("diamond").apply { extendsFrom(IRON) }
        val NETHERITE = register("netherite").apply { extendsFrom(DIAMOND) }
        
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
        
        fun ofItem(item: ItemStack): ToolLevel? {
            val novaLevel = item.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.level
            if (novaLevel != null)
                return novaLevel
            
            return when (item.type) {
                Material.STONE_SWORD, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE -> STONE
                Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE -> IRON
                Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE -> DIAMOND
                Material.NETHERITE_SWORD, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE -> NETHERITE
                else -> null
            }
        }
        
        fun isCorrectLevel(block: Block, tool: ItemStack): Boolean {
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
            return blockLevel == null || blockLevel == toolLevel || (toolLevel != null && toolLevel.doesExtendFrom(blockLevel))
        }
        
    }
    
}