@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item.tool

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.novaBlock

class ToolTier(
    val id: ResourceLocation,
    levelValue: Provider<Double>
) : Comparable<ToolTier> {
    
    val levelValue by levelValue
    
    override fun compareTo(other: ToolTier): Int {
        return levelValue.compareTo(other.levelValue)
    }
    
    companion object {
        
        /**
         * Returns the [ToolTier] of the given [Block].
         * This method works for both vanilla and Nova blocks.
         */
        fun ofBlock(block: Block): ToolTier {
            val novaBlock = block.novaBlock
            if (novaBlock != null)
                return novaBlock.options.toolTier ?: VanillaToolTiers.WOOD
            
            val material = block.type
            return when {
                Tag.NEEDS_STONE_TOOL.isTagged(material) -> VanillaToolTiers.STONE
                Tag.NEEDS_IRON_TOOL.isTagged(material) -> VanillaToolTiers.IRON
                Tag.NEEDS_DIAMOND_TOOL.isTagged(material) -> VanillaToolTiers.DIAMOND
                else -> VanillaToolTiers.WOOD
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
            
            val novaLevel = item.novaItem?.getBehaviorOrNull(Tool::class)?.tier
            if (novaLevel != null)
                return novaLevel
            
            return when (item.type) {
                Material.WOODEN_SWORD, Material.WOODEN_SHOVEL, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE -> VanillaToolTiers.WOOD
                Material.GOLDEN_SWORD, Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE -> VanillaToolTiers.GOLD
                Material.STONE_SWORD, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE -> VanillaToolTiers.STONE
                Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE -> VanillaToolTiers.IRON
                Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE -> VanillaToolTiers.DIAMOND
                Material.NETHERITE_SWORD, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE -> VanillaToolTiers.NETHERITE
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