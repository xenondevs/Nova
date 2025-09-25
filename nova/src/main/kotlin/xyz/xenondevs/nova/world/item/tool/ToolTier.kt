@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.item.tool

import net.kyori.adventure.key.Key
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.novaBlock
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.item.behavior.Tool

class ToolTier(
    val id: Key,
    levelValue: Provider<Double>
) : Comparable<ToolTier> {
    
    val levelValue by levelValue
    
    override fun compareTo(other: ToolTier): Int {
        return levelValue.compareTo(other.levelValue)
    }
    
    override fun toString(): String {
        return id.toString()
    }
    
    companion object {
        
        /**
         * Returns the [ToolTier] of the given [Block].
         * This method works for both vanilla and Nova blocks.
         */
        fun ofBlock(block: Block): ToolTier {
            val novaBlock = block.novaBlock
            if (novaBlock != null)
                return novaBlock.getBehaviorOrNull<Breakable>()?.toolTier ?: VanillaToolTiers.WOOD
            
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
            
            val rules = item.unwrap().get(DataComponents.TOOL)?.rules
                ?: return null
            
            val tiers = HashSet<ToolTier>()
            for (rule in rules) {
                val correctForDrops = rule.correctForDrops
                if (correctForDrops.isEmpty)
                    continue
                
                val key = (rule.blocks as? HolderSet.Named<*>)?.key()
                    ?: continue
                
                if (correctForDrops.get()) {
                    tiers += when (key) {
                        BlockTags.NEEDS_DIAMOND_TOOL -> VanillaToolTiers.DIAMOND
                        BlockTags.NEEDS_IRON_TOOL -> VanillaToolTiers.IRON
                        BlockTags.NEEDS_STONE_TOOL -> VanillaToolTiers.STONE
                        else -> continue
                    }
                } else {
                    tiers += when (key) {
                        BlockTags.INCORRECT_FOR_NETHERITE_TOOL -> VanillaToolTiers.NETHERITE
                        BlockTags.INCORRECT_FOR_DIAMOND_TOOL -> VanillaToolTiers.DIAMOND
                        BlockTags.INCORRECT_FOR_GOLD_TOOL -> VanillaToolTiers.GOLD
                        BlockTags.INCORRECT_FOR_IRON_TOOL -> VanillaToolTiers.IRON
                        BlockTags.INCORRECT_FOR_COPPER_TOOL -> VanillaToolTiers.COPPER
                        BlockTags.INCORRECT_FOR_STONE_TOOL -> VanillaToolTiers.STONE
                        BlockTags.INCORRECT_FOR_WOODEN_TOOL -> VanillaToolTiers.WOOD
                        else -> continue
                    }
                }
            }
            
            return tiers.maxByOrNull { it.levelValue }
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