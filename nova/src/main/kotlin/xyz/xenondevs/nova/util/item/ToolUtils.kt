@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.util.item

import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.util.eyeInWater
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.novaBlock
import xyz.xenondevs.nova.util.roundToDecimalPlaces
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import kotlin.jvm.optionals.getOrNull
import net.minecraft.world.item.component.Tool as MojangTool
import net.minecraft.world.level.block.Block as MojangBlock

object ToolUtils {
    
    fun isCorrectToolForDrops(block: Block, tool: ItemStack?): Boolean {
        val novaBlock = WorldDataManager.getBlockState(block.pos)?.block
        
        // block may not require a tool for drops
        if (novaBlock != null) {
            val requiresToolForDrops = novaBlock.getBehaviorOrNull<Breakable>()
                ?.requiresToolForDrops
                ?: return false // block is not breakable, so there is no correct tool for drops
            if (!requiresToolForDrops)
                return true
        } else if (!(block as CraftBlock).nms.requiresCorrectToolForDrops()) return true
        
        val toolComponent = tool?.unwrap()?.get(DataComponents.TOOL)
        if (toolComponent != null) {
            if (novaBlock != null) {
                // vanilla tool, Nova block
                val blockCategories = novaBlock.getBehaviorOrNull<Breakable>()?.toolCategories ?: emptySet()
                return findMatchingToolComponentRules(toolComponent, blockCategories)
                    .any { it.correctForDrops.getOrNull() == true }
            } else {
                // vanilla tool, vanilla block
                return toolComponent.isCorrectForDrops(block.nmsState)
            }
        } else {
            return ToolCategory.hasCorrectToolCategory(block, tool) && ToolTier.isCorrectLevel(block, tool)
        }
    }
    
    //<editor-fold desc="tool damage", defaultstate="collapsed">
    @Suppress("DEPRECATION")
    internal fun calculateDamage(
        player: Player,
        block: Block,
        tool: ItemStack?
    ): Double {
        when (player.gameMode) {
            GameMode.CREATIVE -> {
                val canBreakBlocks = tool?.novaItem?.getBehaviorOrNull<Tool>()?.canBreakBlocksInCreative
                    ?: ToolCategory.ofItem(tool)
                        .filterIsInstance<VanillaToolCategory>()
                        .takeUnlessEmpty()
                        ?.any { it.canBreakBlocksInCreative }
                    ?: (tool?.type != Material.DEBUG_STICK && tool?.type != Material.TRIDENT)
                
                return if (canBreakBlocks) 1.0 else 0.0
            }
            
            GameMode.ADVENTURE -> tool?.unwrap()
                ?.get(DataComponents.CAN_BREAK)
                ?.test(BlockInWorld(block.world.serverLevel, block.pos.nmsPos, false))
            
            GameMode.SPECTATOR -> return 0.0
            
            else -> Unit
        }
        
        return calculateDamage(
            hardness = block.hardness,
            correctForDrops = isCorrectToolForDrops(block, tool),
            speed = getDestroySpeed(block, tool),
            efficiency = player.getAttribute(Attribute.PLAYER_MINING_EFFICIENCY)?.value ?: 0.0,
            hasteLevel = player.getPotionEffect(PotionEffectType.HASTE)?.amplifier?.plus(1) ?: 0,
            fatigueLevel = player.getPotionEffect(PotionEffectType.MINING_FATIGUE)?.amplifier?.plus(1) ?: 0,
            blockBreakSpeed = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED)?.value ?: 1.0,
            submergedMiningSpeed = player.getAttribute(Attribute.PLAYER_SUBMERGED_MINING_SPEED)?.value ?: 0.2,
            isOnGround = player.isOnGround,
            isUnderWater = player.eyeInWater && player.inventory.helmet?.containsEnchantment(Enchantment.AQUA_AFFINITY) != true
        )
    }
    
    fun calculateDamage(
        hardness: Double,
        correctForDrops: Boolean,
        speed: Double,
        efficiency: Double = 0.0,
        hasteLevel: Int = 0,
        fatigueLevel: Int = 0,
        blockBreakSpeed: Double = 1.0,
        submergedMiningSpeed: Double = 0.2,
        isOnGround: Boolean = true,
        isUnderWater: Boolean = false
    ): Double {
        if (hardness < 0) return 0.0
        if (hardness == 0.0) return 1.0
        
        var speedMultiplier = speed
        
        speedMultiplier += efficiency
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        speedMultiplier *= blockBreakSpeed
        if (isUnderWater) speedMultiplier *= submergedMiningSpeed
        if (!isOnGround) speedMultiplier /= 5.0
        
        return (speedMultiplier / hardness / if (correctForDrops) 30.0 else 100.0).roundToDecimalPlaces(3)
    }
    //</editor-fold>
    
    private fun getDestroySpeed(block: Block, tool: ItemStack?): Double {
        if (tool == null)
            return 1.0
        
        val toolComponent = tool.unwrap().get(DataComponents.TOOL)
        val toolBehavior = tool.novaItem?.getBehaviorOrNull<Tool>()
        if (toolComponent != null) {
            val novaBlock = block.novaBlock
            if (novaBlock != null) {
                // Vanilla tool, Nova block
                // we need to search for a tool rule that matches the block's tool category (tool rules cannot contain custom blocks)
                val blockCategories = novaBlock.getBehaviorOrNull<Breakable>()?.toolCategories ?: emptySet()
                return findMatchingToolComponentRules(toolComponent, blockCategories)
                    .maxOfOrNull { it.speed.orElse(1f) }
                    ?.toDouble() ?: 1.0
            } else {
                // Vanilla tool, Vanilla block
                return toolComponent.getMiningSpeed(block.nmsState).toDouble()
            }
        } else if (toolBehavior != null) {
            // Nova tool, Nova/Vanilla block 
            val itemCategories = toolBehavior.categories
            val blockCategories = ToolCategory.ofBlock(block)
            if (itemCategories.any { it in blockCategories })
                return toolBehavior.breakSpeed
        }
        
        return 1.0
    }
    
    /**
     * Extracts tool component rules from [component] that sort of match any one of the [categories].
     */
    private fun findMatchingToolComponentRules(component: MojangTool, categories: Set<ToolCategory>): Set<MojangTool.Rule> {
        val tags = categories.mapNotNullTo(HashSet(), ::findSimilarTagForToolCategory)
        return component.rules.filterTo(HashSet()) {
            it.blocks is HolderSet.Named && (it.blocks as HolderSet.Named<MojangBlock>).key() in tags 
        }
    }
    
    /**
     * Returns a tag that is similar to the given [category] or null if no such tag exists.
     */
    private fun findSimilarTagForToolCategory(category: ToolCategory): TagKey<MojangBlock>? {
        return when (category) {
            VanillaToolCategories.AXE -> BlockTags.MINEABLE_WITH_AXE
            VanillaToolCategories.HOE -> BlockTags.MINEABLE_WITH_HOE
            VanillaToolCategories.PICKAXE -> BlockTags.MINEABLE_WITH_PICKAXE
            VanillaToolCategories.SHEARS -> BlockTags.LEAVES
            VanillaToolCategories.SHOVEL -> BlockTags.MINEABLE_WITH_SHOVEL
            else -> null
        }
    }
    
    private fun getFatigueMultiplier(level: Int): Double =
        when (level) {
            0 -> 1.0
            1 -> 0.3
            2 -> 0.09
            3 -> 0.0027
            else -> 0.00081
        }
    
}