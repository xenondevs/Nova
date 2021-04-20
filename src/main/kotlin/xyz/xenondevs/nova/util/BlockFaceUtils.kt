package xyz.xenondevs.nova.util

import org.bukkit.Axis
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import kotlin.math.roundToInt

enum class BlockSide(private val rotation: Int, private val blockFace: BlockFace) {
    
    FRONT(0, SOUTH),
    LEFT(1, WEST),
    BACK(2, NORTH),
    RIGHT(3, EAST),
    TOP(-1, UP),
    BOTTOM(-1, DOWN);
    
    fun getBlockFace(yaw: Float): BlockFace {
        if (rotation == -1) return blockFace
        val rot = ((yaw / 90.0).roundToInt() + rotation) % 4
        return values().find { it.rotation == rot }!!.blockFace
    }
    
}

val BlockFace.axis: Axis
    get() = when (this) {
        SOUTH -> Axis.Z
        WEST -> Axis.X
        NORTH -> Axis.Z
        EAST -> Axis.X
        UP -> Axis.Y
        DOWN -> Axis.Y
        
        else -> throw IllegalArgumentException("Illegal facing")
    }

val BlockFace.rotationValues: Pair<Int, Int>
    get() = when (this) {
        NORTH -> 0 to 0
        EAST -> 0 to 1
        SOUTH -> 0 to 2
        WEST -> 0 to 3
        UP -> 1 to 0
        DOWN -> 3 to 0
        
        else -> throw IllegalArgumentException("Illegal facing")
    }
