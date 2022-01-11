package xyz.xenondevs.nova.world

import org.bukkit.Location
import org.bukkit.World

val Location.pos: BlockPos
    get() = BlockPos(world!!, blockX, blockY, blockZ)

data class BlockPos(val world: World, val x: Int, val y: Int, val z: Int)