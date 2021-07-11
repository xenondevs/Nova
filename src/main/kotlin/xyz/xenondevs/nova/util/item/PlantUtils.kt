package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.type.CaveVinesPlant
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.nova.util.hasSameTypeBelow
import kotlin.random.Random

fun Material.isTillable(): Boolean {
    return this == Material.GRASS_BLOCK
        || this == Material.DIRT
        || this == Material.DIRT_PATH
}

fun Block.isHarvestable(): Boolean {
    if (PlantUtils.HARVESTABLE_BLOCKS.containsKey(type))
        return PlantUtils.HARVESTABLE_BLOCKS[type]!!(this)
    
    if (PlantUtils.COMPLEX_HARVESTABLE_BLOCKS.containsKey(type))
        return PlantUtils.COMPLEX_HARVESTABLE_BLOCKS[type]!!.first(this)
    
    return false
}

private fun Block.isFullyAged(): Boolean {
    val blockData = blockData
    return blockData is Ageable && blockData.age == blockData.maximumAge
}

private fun Block.harvestSweetBerries(): ItemStack {
    val data = blockData as Ageable
    data.age = 1
    blockData = data
    
    return ItemStack(Material.SWEET_BERRIES, Random.nextInt(2, 3))
}

private fun Block.canHarvestCaveVines(): Boolean {
    val blockData = blockData
    return blockData is CaveVinesPlant && blockData.isBerries
}

private fun Block.harvestCaveVines(): ItemStack {
    val data = blockData as CaveVinesPlant
    data.isBerries = false
    blockData = data
    
    return ItemStack(Material.GLOW_BERRIES)
}

object PlantUtils {
    
    val PLANTS: Set<Material> = setOf(
        Material.WHEAT_SEEDS,
        Material.BEETROOT_SEEDS,
        Material.POTATO,
        Material.CARROT,
        Material.SWEET_BERRIES,
        Material.PUMPKIN_SEEDS,
        Material.MELON_SEEDS,
        Material.OAK_SAPLING,
        Material.SPRUCE_SAPLING,
        Material.BIRCH_SAPLING,
        Material.JUNGLE_SAPLING,
        Material.ACACIA_SAPLING,
        Material.DARK_OAK_SAPLING
    )
    
    val SEED_BLOCKS: Map<Material, Material> = enumMapOf(
        Material.WHEAT_SEEDS to Material.WHEAT,
        Material.BEETROOT_SEEDS to Material.BEETROOTS,
        Material.POTATO to Material.POTATOES,
        Material.CARROT to Material.CARROTS,
        Material.SWEET_BERRIES to Material.SWEET_BERRY_BUSH,
        Material.PUMPKIN_SEEDS to Material.PUMPKIN_STEM,
        Material.MELON_SEEDS to Material.MELON_STEM
    )
    
    val HARVESTABLE_BLOCKS: Map<Material, (Block.() -> Boolean)?> = enumMapOf(
        Material.GRASS to null,
        Material.TALL_GRASS to null,
        Material.BEE_NEST to null,
        Material.PUMPKIN to null,
        Material.MELON to null,
        Material.WHEAT to Block::isFullyAged,
        Material.BEETROOTS to Block::isFullyAged,
        Material.POTATOES to Block::isFullyAged,
        Material.CARROTS to Block::isFullyAged,
        Material.SWEET_BERRY_BUSH to Block::isFullyAged,
        Material.CACTUS to Block::hasSameTypeBelow,
        Material.SUGAR_CANE to Block::hasSameTypeBelow
    ).also { map ->
        fun addTags(vararg tags: Tag<Material>) =
            tags.forEach { tag -> tag.values.forEach { material -> map[material] = null } }
        
        addTags(Tag.LEAVES, Tag.LOGS, Tag.FLOWERS)
    }
    
    val COMPLEX_HARVESTABLE_BLOCKS: Map<Material, Pair<Block.() -> Boolean, Block.() -> ItemStack>> = enumMapOf(
        Material.SWEET_BERRY_BUSH to (Block::isFullyAged to Block::harvestSweetBerries),
        Material.CAVE_VINES to (Block::canHarvestCaveVines to Block::harvestCaveVines),
        Material.CAVE_VINES_PLANT to (Block::canHarvestCaveVines to Block::harvestCaveVines)
    )
    
}