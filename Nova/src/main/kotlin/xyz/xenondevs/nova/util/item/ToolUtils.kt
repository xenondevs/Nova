package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.util.eyeInWater
import kotlin.random.Random

fun Material.isShovel() = name.endsWith("_SHOVEL")

fun Material.isPickaxe() = name.endsWith("_PICKAXE")

fun Material.isAxe() = name.endsWith("_AXE")

fun Material.isHoe() = name.endsWith("_HOE")

fun Material.isSword() = name.endsWith("_SWORD")

object ToolUtils {
    
    fun damageTool(item: ItemStack): ItemStack? {
        val meta = item.itemMeta
        if (meta is Damageable) {
            if (meta.isUnbreakable)
                return item
            if (meta.hasEnchant(Enchantment.DURABILITY)) {
                val percentage = 100.0 / (meta.getEnchantLevel(Enchantment.DURABILITY) + 1)
                if (Random.nextInt(0, 100) >= percentage)
                    return item
            }
            meta.damage += 1
            if (meta.damage >= item.type.maxDurability)
                return null
            item.itemMeta = meta
        }
        return item
    }
    
    @Suppress("DEPRECATION")
    fun calculateDamage(player: Player, hand: EquipmentSlot, block: Block): Double = calculateDamage(
        block,
        player.inventory.getItem(hand),
        player.isOnGround,
        player.eyeInWater && player.inventory.helmet?.containsEnchantment(Enchantment.WATER_WORKER) != true,
        player.getPotionEffect(PotionEffectType.FAST_DIGGING)?.amplifier?.plus(1) ?: 0,
        player.getPotionEffect(PotionEffectType.SLOW_DIGGING)?.amplifier?.plus(1) ?: 0
    )
    
    fun calculateDamage(block: Block, tool: ItemStack?, onGround: Boolean, underWater: Boolean, hasteLevel: Int, fatigueLevel: Int): Double {
        val blockType = block.type
        val hardness = blockType.hardness.toDouble()
    
        if (hardness < 0) return 0.0
        if (hardness == 0.0) return 1.0
        
        val toolType = tool?.type
        val toolCategory = toolType?.let(ToolCategory::of)
        
        var speedMultiplier = 1.0
        if (toolCategory != null && toolCategory.isCorrectToolCategory(blockType)) {
            speedMultiplier = toolCategory.multipliers[toolType]!!
            val efficiency = tool.getEnchantmentLevel(Enchantment.DIG_SPEED)
            if (efficiency > 0) {
                speedMultiplier += efficiency * efficiency + 1
            }
        }
        
        speedMultiplier *= hasteLevel * 0.2 + 1
        speedMultiplier *= getFatigueMultiplier(fatigueLevel)
        
        if (underWater) speedMultiplier /= 5.0
        if (!onGround) speedMultiplier /= 5.0
        
        return speedMultiplier / hardness / if (isCorrectToolForDrops(toolType, block)) 30.0 else 100.0
    }
    
    @Suppress("DEPRECATION")
    fun calculateDamage(
        player: Player,
        tool: ItemStack,
        toolCategory: ToolCategory?,
        hardness: Double,
        correctCategory: Boolean,
        correctLevel: Boolean,
    ): Double = calculateDamage(
        hardness,
        correctCategory,
        correctLevel,
        toolCategory?.multipliers?.get(tool.type) ?: 0.0,
        tool.getEnchantmentLevel(Enchantment.DIG_SPEED),
        player.isOnGround,
        player.eyeInWater && player.inventory.helmet?.containsEnchantment(Enchantment.WATER_WORKER) != true,
        player.getPotionEffect(PotionEffectType.FAST_DIGGING)?.amplifier?.plus(1) ?: 0,
        player.getPotionEffect(PotionEffectType.SLOW_DIGGING)?.amplifier?.plus(1) ?: 0
    )
    
    fun calculateDamage(
        hardness: Double,
        correctCategory: Boolean,
        correctLevel: Boolean,
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
        
        return speedMultiplier / hardness / if (correctLevel) 30.0 else 100.0
    }
    
    fun isCorrectToolForDrops(tool: Material?, block: Block): Boolean {
        val blockState = (block as CraftBlock).nms
        
        return !blockState.requiresCorrectToolForDrops()
            || (tool != null && CraftMagicNumbers.getItem(tool).isCorrectToolForDrops(blockState))
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

enum class ToolLevel(higherTier: ToolLevel?, vararg materials: Material) {
    
    NETHERITE(null, NETHERITE_SHOVEL, NETHERITE_PICKAXE, NETHERITE_AXE, NETHERITE_HOE, NETHERITE_SWORD),
    DIAMOND(NETHERITE, DIAMOND_SHOVEL, DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_HOE, DIAMOND_SWORD),
    IRON(DIAMOND, IRON_SHOVEL, IRON_PICKAXE, IRON_AXE, IRON_HOE, IRON_SWORD),
    STONE(IRON, STONE_SHOVEL, STONE_PICKAXE, STONE_AXE, STONE_HOE, STONE_SWORD),
    WOODEN(STONE, WOODEN_SHOVEL, WOODEN_PICKAXE, WOODEN_AXE, WOODEN_HOE, WOODEN_SWORD, GOLDEN_SHOVEL, GOLDEN_PICKAXE, GOLDEN_AXE, GOLDEN_HOE, GOLDEN_SWORD);
    
    val materials = HashSet(materials.asList())
    val materialsWithHigherTier: HashSet<Material> =
        if (higherTier != null)
            HashSet<Material>().apply {
                addAll(materials)
                addAll(higherTier.materialsWithHigherTier)
            }
        else this.materials
    
    companion object {
        
        fun isCorrectLevel(block: Material, tool: Material): Boolean {
            val levelTag = when {
                Tag.NEEDS_STONE_TOOL.isTagged(block) -> STONE
                Tag.NEEDS_IRON_TOOL.isTagged(block) -> IRON
                Tag.NEEDS_DIAMOND_TOOL.isTagged(block) -> DIAMOND
                else -> null
            } ?: return true
            
            return tool in levelTag.materialsWithHigherTier
        }
        
    }
    
}

enum class ToolCategory(val multipliers: Map<Material, Double>, val isCorrectToolCategory: (Material) -> Boolean) {
    
    SHOVEL(
        mapOf(
            WOODEN_SHOVEL to 2.0,
            STONE_SHOVEL to 4.0,
            IRON_SHOVEL to 6.0,
            DIAMOND_SHOVEL to 8.0,
            NETHERITE_SHOVEL to 9.0,
            GOLDEN_SHOVEL to 12.0
        ),
        { Tag.MINEABLE_SHOVEL.isTagged(it) }
    ),
    
    PICKAXE(
        mapOf(
            WOODEN_PICKAXE to 2.0,
            STONE_PICKAXE to 4.0,
            IRON_PICKAXE to 6.0,
            DIAMOND_PICKAXE to 8.0,
            NETHERITE_PICKAXE to 9.0,
            GOLDEN_PICKAXE to 12.0
        ),
        { Tag.MINEABLE_PICKAXE.isTagged(it) }
    ),
    
    AXE(
        mapOf(
            WOODEN_AXE to 2.0,
            STONE_AXE to 4.0,
            IRON_AXE to 6.0,
            DIAMOND_AXE to 8.0,
            NETHERITE_AXE to 9.0,
            GOLDEN_AXE to 12.0
        ),
        { Tag.MINEABLE_AXE.isTagged(it) }
    ),
    
    HOE(
        mapOf(
            WOODEN_HOE to 2.0,
            STONE_HOE to 4.0,
            IRON_HOE to 6.0,
            DIAMOND_HOE to 8.0,
            NETHERITE_HOE to 9.0,
            GOLDEN_HOE to 12.0
        ),
        { Tag.MINEABLE_HOE.isTagged(it) }
    ),
    
    SWORD(
        mapOf(
            WOODEN_SWORD to 1.5,
            STONE_SWORD to 1.5,
            IRON_SWORD to 1.5,
            DIAMOND_SWORD to 1.5,
            NETHERITE_SWORD to 1.5,
            GOLDEN_SWORD to 1.5
        ),
        { it == COBWEB || it == BAMBOO }
    ),
    
    SHEARS(
        mapOf(
            Material.SHEARS to 1.5
        ),
        { Tag.LEAVES.isTagged(it) || Tag.WOOL.isTagged(it) || it == COBWEB }
    );
    
    companion object {
        
        fun of(material: Material): ToolCategory? =
            when (material) {
                WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL, GOLDEN_SHOVEL -> SHOVEL
                WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE, GOLDEN_PICKAXE -> PICKAXE
                WOODEN_AXE, STONE_AXE, IRON_AXE, DIAMOND_AXE, NETHERITE_AXE, GOLDEN_AXE -> AXE
                WOODEN_HOE, STONE_HOE, IRON_HOE, DIAMOND_HOE, NETHERITE_HOE, GOLDEN_HOE -> HOE
                WOODEN_SWORD, STONE_SWORD, IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD, GOLDEN_SWORD -> SWORD
                Material.SHEARS -> SHEARS
                
                else -> null
            }
        
    }
    
}
