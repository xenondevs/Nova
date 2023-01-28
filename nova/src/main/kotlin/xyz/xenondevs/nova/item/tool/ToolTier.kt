@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolTierRegistry.register
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.novaMaterial

class ToolTier(
    val id: NamespacedId,
    levelValue: Provider<Double>
) : Comparable<ToolTier> {
    
    val levelValue by levelValue
    
    override fun compareTo(other: ToolTier): Int {
        return levelValue.compareTo(other.levelValue)
    }
    
    companion object {
        
        val WOOD = register("wood")
        val GOLD = register("gold")
        val STONE = register("stone")
        val IRON = register("iron")
        val DIAMOND = register("diamond")
        val NETHERITE = register("netherite")
        
        /**
         * Returns the [ToolTier] of the given [Block].
         * This method works for both vanilla and Nova blocks.
         */
        fun ofBlock(block: Block): ToolTier {
            val novaMaterial = block.novaMaterial
            if (novaMaterial != null)
                return novaMaterial.toolTier ?: WOOD
            
            val material = block.type
            return when {
                Tag.NEEDS_STONE_TOOL.isTagged(material) -> STONE
                Tag.NEEDS_IRON_TOOL.isTagged(material) -> IRON
                Tag.NEEDS_DIAMOND_TOOL.isTagged(material) -> DIAMOND
                else -> WOOD
            }
        }
        
        /**
         * Returns the [ToolTier] of the given [ItemStack].
         * This method works for both vanilla and Nova items.
         * If the provided [ItemStack] is not a tool, null will be returned.
         */
        fun ofItem(item: ItemStack?): ToolTier? {
            if (item == null)
                return null
            
            val novaLevel = item.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.tier
            if (novaLevel != null)
                return novaLevel
            
            return when (item.type) {
                Material.WOODEN_SWORD, Material.WOODEN_SHOVEL, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE -> WOOD
                Material.GOLDEN_SWORD, Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE -> GOLD
                Material.STONE_SWORD, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE -> STONE
                Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE -> IRON
                Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE -> DIAMOND
                Material.NETHERITE_SWORD, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE -> NETHERITE
                else -> null
            }
        }
        
        fun isCorrectLevel(block: Block, tool: ItemStack?): Boolean {
            val toolLevel = ofItem(tool)
            val blockLevel = ofBlock(block)
            return isCorrectLevel(blockLevel, toolLevel)
        }
        
        fun isCorrectLevel(blockTier: ToolTier?, toolTier: ToolTier?): Boolean {
            return isCorrectLevel(blockTier?.levelValue, toolTier?.levelValue)
        }
        
        fun isCorrectLevel(blockLevel: Double?, toolLevel: Double?): Boolean {
            return blockLevel == null || (toolLevel ?: 0.0) >= blockLevel
        }
        
    }
    
}