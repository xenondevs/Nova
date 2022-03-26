package xyz.xenondevs.nova.world

import org.bukkit.Location
import org.bukkit.World
import xyz.xenondevs.nova.util.Location

val Location.pos: BlockPos
    get() = BlockPos(world!!, blockX, blockY, blockZ)

data class BlockPos(val world: World, val x: Int, val y: Int, val z: Int) {
    val location: Location
        get() = Location(world, x, y, z)
}