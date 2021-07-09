package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import xyz.xenondevs.nova.util.enumMapOf

fun Material.isTillable(): Boolean {
    return this == Material.FARMLAND
        || this == Material.GRASS_BLOCK
        || this == Material.DIRT
        || this == Material.DIRT_PATH
}

object PlantUtils {
    
    val SEED_BLOCKS = enumMapOf(
        Material.WHEAT_SEEDS to Material.WHEAT,
        Material.BEETROOT_SEEDS to Material.BEETROOTS,
        Material.POTATO to Material.POTATOES,
        Material.CARROT to Material.CARROTS,
        Material.PUMPKIN_SEEDS to Material.PUMPKIN_STEM,
        Material.MELON_SEEDS to Material.MELON_STEM
    )
    
}