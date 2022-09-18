package xyz.xenondevs.nova.util.item

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
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
import xyz.xenondevs.nova.util.eyeInWater
import xyz.xenondevs.nova.util.getPlayersNearby
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random
import org.bukkit.inventory.meta.Damageable as BukkitDamageable
import xyz.xenondevs.nova.item.behavior.Damageable as NovaDamageable

fun Player.damageToolInMainHand(damage: Int = 1) {
    val inventory = inventory
    val item = inventory.getItem(EquipmentSlot.HAND)?.takeUnlessAir() ?: return
    val damagedTool = ToolUtils.damageTool(item, damage)
    
    if (damagedTool == null) {
        val breakPacket = ClientboundEntityEventPacket(nmsEntity, 47.toByte())
        location.getPlayersNearby(32.0).forEach { it.send(breakPacket) }
    }
    
    inventory.setItem(EquipmentSlot.HAND, damagedTool)
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
        
        if (meta.hasEnchant(Enchantment.DURABILITY)) {
            val percentage = 100.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1)
            if (Random.nextInt(0, 100) >= percentage)
                return item
        }
        
        val novaDamageable = item.novaMaterial?.novaItem?.getBehavior(NovaDamageable::class)
        if (novaDamageable != null) {
            novaDamageable.addDamage(item, damage)
            if (novaDamageable.getDamage(item) >= novaDamageable.maxDurability)
                return null
        } else if (meta is BukkitDamageable && item.type.maxDurability > 0) {
            meta.damage += damage
            if (meta.damage >= item.type.maxDurability)
                return null
            item.itemMeta = meta
        }
        
        return item
    }
    
    fun isCorrectToolForDrops(tool: ItemStack?, block: Block): Boolean {
        val novaBlock = BlockManager.getBlock(block.pos)
        if (novaBlock != null) {
            if (!novaBlock.material.requiresToolForDrops)
                return true
        } else if (!requiresCorrectToolForDropsVanilla(block)) return true
        
        if (tool == null)
            return false
        
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