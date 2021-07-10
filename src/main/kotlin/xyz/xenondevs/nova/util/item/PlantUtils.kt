package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import xyz.xenondevs.nova.util.enumMapOf

fun Material.isTillable(): Boolean {
    return this == Material.GRASS_BLOCK
        || this == Material.DIRT
        || this == Material.DIRT_PATH
}

object PlantUtils {
    
    val PLANTS = setOf(
        Material.WHEAT_SEEDS,
        Material.BEETROOT_SEEDS,
        Material.POTATO,
        Material.CARROT,
        Material.PUMPKIN_SEEDS,
        Material.MELON_SEEDS,
        Material.OAK_SAPLING,
        Material.SPRUCE_SAPLING,
        Material.BIRCH_SAPLING,
        Material.JUNGLE_SAPLING,
        Material.ACACIA_SAPLING,
        Material.DARK_OAK_SAPLING
    )
    
    val SEED_BLOCKS = enumMapOf(
        Material.WHEAT_SEEDS to Material.WHEAT,
        Material.BEETROOT_SEEDS to Material.BEETROOTS,
        Material.POTATO to Material.POTATOES,
        Material.CARROT to Material.CARROTS,
        Material.PUMPKIN_SEEDS to Material.PUMPKIN_STEM,
        Material.MELON_SEEDS to Material.MELON_STEM
    )
    
}