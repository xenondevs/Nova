@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.util.item

import io.papermc.paper.datacomponent.DataComponentTypes
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.block.state.pattern.BlockInWorld
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.util.eyeInWater
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.roundToDecimalPlaces
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.item.novaItem

object ToolUtils {
    
    @Deprecated("Equivalent Bukkit API exists", ReplaceWith("block.isPreferredTool(tool ?: ItemStack.empty())", "org.bukkit.inventory.ItemStack"))
    fun isCorrectToolForDrops(block: Block, tool: ItemStack?): Boolean =
        block.isPreferredTool(tool ?: ItemStack.empty())
    
    @Suppress("DEPRECATION")
    internal fun calculateDamage(
        player: Player,
        block: Block,
        tool: ItemStack?
    ): Double {
        when (player.gameMode) {
            GameMode.CREATIVE -> {
                val canBreakBlocks = tool?.getData(DataComponentTypes.TOOL)?.canDestroyBlocksInCreative()
                    ?: true
                
                return if (canBreakBlocks) 1.0 else 0.0
            }
            
            GameMode.ADVENTURE -> tool?.unwrap()
                ?.get(DataComponents.CAN_BREAK)
                ?.test(BlockInWorld(block.world.serverLevel, block.nmsPos, false))
            
            GameMode.SPECTATOR -> return 0.0
            
            else -> Unit
        }
        
        var damage = calculateDamage(
            hardness = block.blockType.hardness.toDouble(),
            correctForDrops = isCorrectToolForDrops(block, tool),
            speed = tool?.let { block.getDestroySpeed(it) }?.toDouble() ?: 1.0,
            efficiency = player.getAttribute(Attribute.MINING_EFFICIENCY)?.value ?: 0.0,
            hasteLevel = player.getPotionEffect(PotionEffectType.HASTE)?.amplifier?.plus(1) ?: 0,
            fatigueLevel = player.getPotionEffect(PotionEffectType.MINING_FATIGUE)?.amplifier?.plus(1) ?: 0,
            blockBreakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED)?.value ?: 1.0,
            submergedMiningSpeed = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED)?.value ?: 0.2,
            isOnGround = player.isOnGround,
            isUnderWater = player.eyeInWater && !player.inventory.helmet.containsEnchantment(Enchantment.AQUA_AFFINITY)
        )
        
        val novaItem = tool?.novaItem
        if (novaItem != null) {
            damage = novaItem.modifyBlockDamage(player, tool, block, damage)
        }
        
        return damage
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
        
        if (speedMultiplier > 1) speedMultiplier += efficiency
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        speedMultiplier *= blockBreakSpeed
        if (isUnderWater) speedMultiplier *= submergedMiningSpeed
        if (!isOnGround) speedMultiplier /= 5.0
        
        return (speedMultiplier / hardness / if (correctForDrops) 30.0 else 100.0).roundToDecimalPlaces(3)
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
