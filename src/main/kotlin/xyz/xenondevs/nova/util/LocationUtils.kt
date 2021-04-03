package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager

val CUBE_FACES = listOf(NORTH, EAST, SOUTH, WEST, UP, DOWN)

val Location.blockLocation: Location
    get() = Location(world, blockX.toDouble(), blockY.toDouble(), blockZ.toDouble())

fun Location.dropItems(items: Iterable<ItemStack>) {
    val world = world!!
    items.forEach { world.dropItemNaturally(this, it) }
}

fun Location.removeOrientation() {
    yaw = 0f
    pitch = 0f
}

fun Location.advance(blockFace: BlockFace, stepSize: Double = 1.0) =
    add(
        blockFace.modX.toDouble() * stepSize,
        blockFace.modY.toDouble() * stepSize,
        blockFace.modZ.toDouble() * stepSize
    )

fun Location.getNeighboringTileEntities(): Map<BlockFace, TileEntity> {
    return getNeighboringTileEntitiesOfType()
}

inline fun <reified T> Location.getNeighboringTileEntitiesOfType(): Map<BlockFace, T> {
    val tileEntities = HashMap<BlockFace, T>()
    CUBE_FACES.forEach {
        val location = blockLocation.advance(it)
        val tileEntity = TileEntityManager.getTileEntityAt(location)
        if (tileEntity != null && tileEntity is T) tileEntities[it] = tileEntity as T
    }
    
    return tileEntities
}