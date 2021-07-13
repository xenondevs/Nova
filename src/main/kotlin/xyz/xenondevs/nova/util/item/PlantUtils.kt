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

fun Material.requiresFarmland(): Boolean {
    val requiredTypes = PlantUtils.PLANTS[this]
    return requiredTypes != null && requiredTypes.size == 1 && Material.FARMLAND in requiredTypes
}

fun Material.canBePlacedOn(soilType: Material): Boolean {
    val requiredTypes = PlantUtils.PLANTS[this]
    return requiredTypes != null && soilType in requiredTypes
}

fun Material.isLeaveLike(): Boolean {
    return Tag.LEAVES.isTagged(this) || Tag.WART_BLOCKS.isTagged(this)
}

fun Material.isTreeAttachment(): Boolean {
    return this == Material.BEE_NEST
        || this == Material.SHROOMLIGHT
        || this == Material.WEEPING_VINES_PLANT
        || this == Material.WEEPING_VINES
}

fun Block.isHarvestable(): Boolean {
    if (PlantUtils.HARVESTABLE_BLOCKS.containsKey(type))
        return PlantUtils.HARVESTABLE_BLOCKS[type]?.invoke(this) ?: true
    
    if (PlantUtils.COMPLEX_HARVESTABLE_BLOCKS.containsKey(type))
        return PlantUtils.COMPLEX_HARVESTABLE_BLOCKS[type]!!.first(this)
    
    return false
}

fun Block.isFullyAged(): Boolean {
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
    
    val PLANTS: Map<Material, Set<Material>>
    
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
        Material.SHROOMLIGHT to null,
        Material.WEEPING_VINES to null,
        Material.WEEPING_VINES_PLANT to null,
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
        
        addTags(Tag.LEAVES, Tag.LOGS, Tag.FLOWERS, Tag.WART_BLOCKS)
    }
    
    val COMPLEX_HARVESTABLE_BLOCKS: Map<Material, Pair<Block.() -> Boolean, Block.() -> ItemStack>> = enumMapOf(
        Material.SWEET_BERRY_BUSH to (Block::isFullyAged to Block::harvestSweetBerries),
        Material.CAVE_VINES to (Block::canHarvestCaveVines to Block::harvestCaveVines),
        Material.CAVE_VINES_PLANT to (Block::canHarvestCaveVines to Block::harvestCaveVines)
    )
    
    init {
        val farmland = setOf(Material.FARMLAND)
        val defaultDirts = setOf(Material.FARMLAND, Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.ROOTED_DIRT, Material.PODZOL, Material.MYCELIUM)
        
        PLANTS = mapOf(
            Material.WHEAT_SEEDS to farmland,
            Material.BEETROOT_SEEDS to farmland,
            Material.POTATO to farmland,
            Material.CARROT to farmland,
            Material.PUMPKIN_SEEDS to farmland,
            Material.MELON_SEEDS to farmland,
            Material.SWEET_BERRIES to defaultDirts,
            Material.OAK_SAPLING to defaultDirts,
            Material.SPRUCE_SAPLING to defaultDirts,
            Material.BIRCH_SAPLING to defaultDirts,
            Material.JUNGLE_SAPLING to defaultDirts,
            Material.ACACIA_SAPLING to defaultDirts,
            Material.DARK_OAK_SAPLING to defaultDirts,
            Material.CRIMSON_FUNGUS to setOf(Material.CRIMSON_NYLIUM),
            Material.WARPED_FUNGUS to setOf(Material.WARPED_NYLIUM)
        )
    }
    
}