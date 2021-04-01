package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import org.bukkit.inventory.ItemStack

val CUBE_FACES = listOf(NORTH, EAST, SOUTH, WEST, UP, DOWN)

fun Location.dropItems(items: Iterable<ItemStack>) {
    val world = world!!
    items.forEach { world.dropItemNaturally(this, it) }
}

fun Location.removeOrientation() {
    yaw = 0f
    pitch = 0f
}

fun Location.getBlockLocation(): Location {
    val clone = clone()
    clone.yaw = 0f
    clone.pitch = 0f
    clone.x = blockX.toDouble()
    clone.y = blockY.toDouble()
    clone.z = blockZ.toDouble()
    
    return clone
}