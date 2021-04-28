package xyz.xenondevs.nova.util

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager

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
            ?: VanillaTileEntityManager.getTileEntityAt(location)
        if (tileEntity != null && tileEntity is T) tileEntities[it] = tileEntity as T
    }
    
    return tileEntities
}

fun Location.castRay(stepSize: Double, maxDistance: Double, run: (Location) -> Boolean) {
    val vector = direction.multiply(stepSize)
    val location = clone()
    var distance = 0.0
    while (run(location)) {
        location.add(vector)
        distance += stepSize
        if (distance > maxDistance) break
    }
}

fun Chunk.getSurroundingChunks(range: Int, includeCurrent: Boolean): List<Chunk> {
    val chunks = ArrayList<Chunk>()
    val world = world
    for (chunkX in (x - 1)..(x + 1)) {
        for (chunkZ in (z - 1)..(z + 1)) {
            val chunk = world.getChunkAt(chunkX, chunkZ)
            if (chunk != this || includeCurrent)
                chunks += world.getChunkAt(chunkX, chunkZ)
        }
    }
    
    return chunks
}

fun Location.untilHeightLimit(includeThis: Boolean, run: (Location) -> Boolean) {
    val heightLimit = world!!.maxHeight
    val location = clone().apply { if (!includeThis) add(0.0, 1.0, 0.0) }
    while (location.y < heightLimit) {
        if (!run(location)) break
        
        location.add(0.0, 1.0, 0.0)
    }
}