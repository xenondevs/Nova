@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.util.item

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock
import org.bukkit.craftbukkit.v1_19_R2.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.item.tool.VanillaToolCategory
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.eyeInWater
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.roundToDecimalPlaces
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.item.ItemStack as MojangStack
import xyz.xenondevs.nova.item.behavior.Damageable as NovaDamageable

/**
 * Damages the tool in the [player's][Player] main hand as if they've broken a block.
 */
fun Player.damageToolBreakBlock() = damageToolInMainHand {
    val novaItem = it.novaMaterial?.novaItem
    if (novaItem != null) {
        if (novaItem.hasBehavior(Tool::class)) {
            novaItem.getBehavior(NovaDamageable::class)?.options?.itemDamageOnBreakBlock ?: 0
        } else 0
    } else (ToolCategory.ofItem(it.bukkitMirror) as? VanillaToolCategory)?.itemDamageOnBreakBlock ?: 0
}

/**
 * Damages the tool in the [player's][Player] main hand as if they've attack an entity.
 */
fun Player.damageToolAttackEntity() = damageToolInMainHand {
    val novaItem = it.novaMaterial?.novaItem
    if (novaItem != null) {
        if (novaItem.hasBehavior(Tool::class)) {
            novaItem.getBehavior(NovaDamageable::class)?.options?.itemDamageOnAttackEntity ?: 0
        } else 0
    } else (ToolCategory.ofItem(it.bukkitMirror) as? VanillaToolCategory)?.itemDamageOnAttackEntity ?: 0
}

private inline fun Player.damageToolInMainHand(damageReceiver: (MojangStack) -> Int) {
    val serverPlayer = serverPlayer
    val itemStack = serverPlayer.mainHandItem
    val damage = damageReceiver(itemStack)
    
    if (damage <= 0)
        return
    
    if (DamageableUtils.damageAndBreakItem(itemStack, damage, serverPlayer) == ItemDamageResult.BROKEN) {
        serverPlayer.broadcastBreakEvent(MojangEquipmentSlot.MAINHAND)
    }
}

object ToolUtils {
    
    fun isCorrectToolForDrops(block: Block, tool: ItemStack?): Boolean {
        val novaBlock = BlockManager.getBlock(block.pos)
        if (novaBlock != null) {
            if (!novaBlock.material.requiresToolForDrops)
                return true
        } else if (!requiresCorrectToolForDropsVanilla(block)) return true
        
        val blockToolCategories = ToolCategory.ofBlock(block)
        val blockToolLevel = ToolTier.ofBlock(block)
        val itemToolCategory = ToolCategory.ofItem(tool)
        val itemToolLevel = ToolTier.ofItem(tool)
        
        return itemToolCategory in blockToolCategories && ToolTier.isCorrectLevel(blockToolLevel, itemToolLevel)
    }
    
    //<editor-fold desc="vanilla tool damage", defaultstate="collapsed">
    internal fun isCorrectToolForDropsVanilla(tool: Material?, block: Block): Boolean {
        val blockState = (block as CraftBlock).nms
        
        return !blockState.requiresCorrectToolForDrops()
            || (tool != null && CraftMagicNumbers.getItem(tool).isCorrectToolForDrops(blockState))
    }
    
    internal fun requiresCorrectToolForDropsVanilla(block: Block): Boolean {
        return (block as CraftBlock).nms.requiresCorrectToolForDrops()
    }
    
    @Suppress("DEPRECATION")
    internal fun calculateDamageVanilla(player: Player, block: Block): Double {
        val serverTool = player.inventory.getItem(EquipmentSlot.HAND)?.takeUnlessEmpty()
        val tool = serverTool?.let {
            val nmsStack = it.nmsCopy
            return@let if (PacketItems.isNovaItem(nmsStack))
                PacketItems.getFakeItem(player, nmsStack).bukkitMirror
            else it
        }
        
        when (player.gameMode) {
            GameMode.CREATIVE -> return when (tool?.type) {
                // Players in creative cannot break blocks with those items
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.DEBUG_STICK, Material.TRIDENT -> 0.0
                
                else -> 1.0
            }
            
            GameMode.ADVENTURE -> if (tool == null || block.type !in tool.canDestroy) return 0.0
            GameMode.SPECTATOR -> return 0.0
            
            else -> Unit
        }
        
        return calculateDamageVanilla(
            block,
            tool,
            player.isOnGround,
            player.eyeInWater && player.inventory.helmet?.containsEnchantment(Enchantment.WATER_WORKER) != true,
            player.getPotionEffect(PotionEffectType.FAST_DIGGING)?.amplifier?.plus(1) ?: 0,
            player.getPotionEffect(PotionEffectType.SLOW_DIGGING)?.amplifier?.plus(1) ?: 0
        )
    }
    
    private fun calculateDamageVanilla(block: Block, tool: ItemStack?, onGround: Boolean, underWater: Boolean, hasteLevel: Int, fatigueLevel: Int): Double {
        val blockType = block.type
        val hardness = blockType.hardness.toDouble()
        
        if (hardness < 0) return 0.0
        if (hardness == 0.0) return 1.0
        
        val toolCategory = tool?.let(ToolCategory::ofItem)
        var speedMultiplier = 1.0
        if (toolCategory != null && toolCategory in ToolCategory.ofVanillaBlock(block)) {
            toolCategory as VanillaToolCategory
            val toolType = tool.type
            speedMultiplier = toolCategory.specialMultipliers[toolType]?.get(blockType) ?: toolCategory.genericMultipliers[toolType] ?: 1.0
            val efficiency = tool.getEnchantmentLevel(Enchantment.DIG_SPEED)
            if (efficiency > 0) {
                speedMultiplier += efficiency * efficiency + 1
            }
        }
        
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        
        if (underWater) speedMultiplier /= 5.0
        if (!onGround) speedMultiplier /= 5.0
        
        return (speedMultiplier / hardness / if (isCorrectToolForDropsVanilla(tool?.type, block)) 30.0 else 100.0).roundToDecimalPlaces(3)
    }
    //</editor-fold>
    
    //<editor-fold desc="tool damage", defaultstate="collapsed">
    @Suppress("DEPRECATION")
    internal fun calculateDamage(
        player: Player,
        block: Block,
        tool: ItemStack?,
        hardness: Double,
        correctCategory: Boolean,
        correctForDrops: Boolean,
    ): Double {
        when (player.gameMode) {
            
            GameMode.CREATIVE -> {
                val canBreakBlocks = tool?.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.canBreakBlocksInCreative
                    ?: (ToolCategory.ofItem(tool) as? VanillaToolCategory)?.canBreakBlocksInCreative
                    ?: (tool?.type != Material.DEBUG_STICK && tool?.type != Material.TRIDENT)
                
                return if (canBreakBlocks) 1.0 else 0.0
            }
            
            GameMode.ADVENTURE -> if (tool != null && block.type !in tool.canDestroy) return 0.0
            GameMode.SPECTATOR -> return 0.0
            
            else -> Unit
        }
        
        return calculateDamage(
            hardness,
            correctCategory,
            correctForDrops,
            getToolSpeedMultiplier(tool, block),
            tool?.getEnchantmentLevel(Enchantment.DIG_SPEED) ?: 0,
            player.isOnGround,
            player.eyeInWater && player.inventory.helmet?.containsEnchantment(Enchantment.WATER_WORKER) != true,
            player.getPotionEffect(PotionEffectType.FAST_DIGGING)?.amplifier?.plus(1) ?: 0,
            player.getPotionEffect(PotionEffectType.SLOW_DIGGING)?.amplifier?.plus(1) ?: 0
        )
    }
    
    fun calculateDamage(
        hardness: Double,
        correctCategory: Boolean,
        correctForDrops: Boolean,
        toolMultiplier: Double,
        efficiency: Int,
        onGround: Boolean,
        underWater: Boolean,
        hasteLevel: Int,
        fatigueLevel: Int
    ): Double {
        if (hardness < 0) return 0.0
        if (hardness == 0.0) return 1.0
        
        var speedMultiplier = 1.0
        if (correctCategory) {
            speedMultiplier = toolMultiplier
            if (efficiency > 0)
                speedMultiplier += efficiency * efficiency + 1
        }
        
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        
        if (underWater) speedMultiplier /= 5.0
        if (!onGround) speedMultiplier /= 5.0
        
        return (speedMultiplier / hardness / if (correctForDrops) 30.0 else 100.0).roundToDecimalPlaces(3)
    }
    //</editor-fold>
    
    private fun getToolSpeedMultiplier(itemStack: ItemStack?, block: Block): Double {
        if (itemStack == null)
            return 1.0
        
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null)
            return novaMaterial.novaItem.getBehavior(Tool::class)?.options?.breakSpeed ?: 1.0
        
        val vanillaToolCategory = ToolCategory.ofItem(itemStack) as? VanillaToolCategory
        if (vanillaToolCategory != null) {
            val itemType = itemStack.type
            if (BlockManager.getBlock(block.pos) == null) {
                val specialMultiplier = vanillaToolCategory.specialMultipliers[itemType]?.get(block.type)
                if (specialMultiplier != null)
                    return specialMultiplier
            }
            
            return vanillaToolCategory.genericMultipliers[itemType] ?: 1.0
        }
        
        return 1.0
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