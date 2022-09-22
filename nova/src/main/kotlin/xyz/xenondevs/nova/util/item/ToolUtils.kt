package xyz.xenondevs.nova.util.item

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.eyeInWater
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.meta.Damageable as BukkitDamageable
import xyz.xenondevs.nova.item.behavior.Damageable as NovaDamageable

fun Player.damageToolInMainHand(damage: Int = 1) {
    val serverPlayer = serverPlayer
    if (ToolUtils.damageAndBreakTool(serverPlayer.mainHandItem, damage, serverPlayer) == ToolDamageResult.BROKEN) {
        serverPlayer.broadcastBreakEvent(MojangEquipmentSlot.MAINHAND)
    }
}

object ToolUtils {
    
    /**
     * Damages the given [item] while taking the unbreaking enchantment and unbreakable property into account.
     *
     * This method works for both Vanilla and Nova tools.
     *
     * @return The same [ItemStack] with the durability possibly reduced or null if the item was broken.
     */
    fun damageTool(item: ItemStack, damage: Int = 1): ItemStack? {
        val meta = item.itemMeta ?: return item
        
        if (meta.isUnbreakable)
            return item
        
        val unbreakingLevel = meta.getEnchantLevel(Enchantment.DURABILITY)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return item
        
        val novaDamageable = item.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null) {
            val newDamage = novaDamageable.getDamage(item) + damage
            novaDamageable.setDamage(item, newDamage)
            if (newDamage >= novaDamageable.maxDurability)
                return null
        } else if (meta is BukkitDamageable && item.type.maxDurability > 0) {
            meta.damage += damage
            if (meta.damage >= item.type.maxDurability)
                return null
            item.itemMeta = meta
        }
        
        return item
    }
    
    /**
     * Damages an [itemStack] with damage amount [damage] for [player].
     *
     * This method takes the unbreaking enchantment into consideration and also calls the item_durability_changed
     * criteria trigger if [player] is not null.
     *
     * @return If the item is now broken
     */
    internal fun damageAndBreakTool(itemStack: MojangStack, damage: Int, entity: LivingEntity?): ToolDamageResult {
        if (entity is MojangPlayer && entity.abilities.instabuild)
            return ToolDamageResult.UNDAMAGED
        
        val unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack)
        if (unbreakingLevel > 0 && Random.nextInt(0, unbreakingLevel + 1) > 0)
            return ToolDamageResult.UNDAMAGED
        
        var broken = false
        
        val novaDamageable = itemStack.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null) {
            val bukkitStack = itemStack.bukkitMirror
            
            if (entity is ServerPlayer)
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(entity, itemStack, novaDamageable.getDamage(bukkitStack) + damage)
            
            val newDamage = novaDamageable.getDamage(bukkitStack) + damage
            novaDamageable.setDamage(bukkitStack, newDamage)
            if (newDamage >= novaDamageable.maxDurability)
                broken = true
        } else if (itemStack.isDamageableItem) {
            if (entity is ServerPlayer)
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(entity, itemStack, itemStack.damageValue + damage)
            
            itemStack.damageValue += damage
            if (itemStack.damageValue >= itemStack.maxDamage)
                broken = true
        } else return ToolDamageResult.UNDAMAGED
        
        if (broken) {
            itemStack.shrink(1)
            if (entity is MojangPlayer && novaDamageable != null) {
                entity.awardStat(Stats.ITEM_BROKEN.get(itemStack.item))
            }
        }
        
        return if (broken) ToolDamageResult.BROKEN else ToolDamageResult.DAMAGED
    }
    
    fun isCorrectToolForDrops(tool: ItemStack?, block: Block): Boolean {
        val novaBlock = BlockManager.getBlock(block.pos)
        if (novaBlock != null) {
            if (!novaBlock.material.requiresToolForDrops)
                return true
        } else if (!requiresCorrectToolForDropsVanilla(block)) return true
        
        val blockToolCategories = ToolCategory.ofBlock(block)
        val blockToolLevel = ToolLevel.ofBlock(block)
        val itemToolCategory = ToolCategory.ofItem(tool)
        val itemToolLevel = ToolLevel.ofItem(tool)
        
        return itemToolCategory in blockToolCategories && ToolLevel.isCorrectLevel(blockToolLevel, itemToolLevel)
    }
    
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
        val tool = player.inventory.getItem(EquipmentSlot.HAND)?.takeUnlessAir()
        
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
        if (toolCategory != null && toolCategory.isCorrectToolCategoryForBlock(block)) {
            speedMultiplier = toolCategory.getMultiplier(tool)
            val efficiency = tool.getEnchantmentLevel(Enchantment.DIG_SPEED)
            if (efficiency > 0) {
                speedMultiplier += efficiency * efficiency + 1
            }
        }
        
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        
        if (underWater) speedMultiplier /= 5.0
        if (!onGround) speedMultiplier /= 5.0
        
        return speedMultiplier / hardness / if (isCorrectToolForDropsVanilla(tool?.type, block)) 30.0 else 100.0
    }
    
    
    @Suppress("DEPRECATION")
    internal fun calculateDamage(
        player: Player,
        block: Block,
        tool: ItemStack,
        toolCategory: ToolCategory?,
        hardness: Double,
        correctCategory: Boolean,
        correctForDrops: Boolean,
    ): Double {
        when (player.gameMode) {
            GameMode.CREATIVE -> return if (
            // Players in creative cannot break blocks with those items
                ToolCategory.ofItem(tool) == ToolCategory.SWORD
                || tool.type == Material.DEBUG_STICK
                || tool.type == Material.TRIDENT
            ) 0.0 else 1.0
            
            GameMode.ADVENTURE -> if (block.type !in tool.canDestroy) return 0.0
            GameMode.SPECTATOR -> return 0.0
            
            else -> Unit
        }
        
        return calculateDamage(
            hardness,
            correctCategory,
            correctForDrops,
            toolCategory?.getMultiplier?.invoke(tool) ?: 0.0,
            tool.getEnchantmentLevel(Enchantment.DIG_SPEED),
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
        
        return speedMultiplier / hardness / if (correctForDrops) 30.0 else 100.0
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

internal enum class ToolDamageResult {
    UNDAMAGED,
    DAMAGED,
    BROKEN
}