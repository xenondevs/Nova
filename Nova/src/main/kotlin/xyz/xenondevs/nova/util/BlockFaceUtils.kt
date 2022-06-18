package xyz.xenondevs.nova.util

import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import xyz.xenondevs.nova.util.item.isTraversable
import kotlin.math.absoluteValue
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
        
        val rot = ((yaw / 90.0).roundToInt() + rotation).mod(4)
        return values()[rot].blockFace
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

val BlockFace.mod: Int
    get() = when (this.axis) {
        Axis.X -> modX
        Axis.Y -> modY
        Axis.Z -> modZ
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

val BlockFace.yaw: Float
    get() = when (this) {
        SOUTH -> 0f
        WEST -> 90f
        NORTH -> 180f
        EAST -> 270f
        UP -> 0f
        DOWN -> 0f
        
        else -> throw UnsupportedOperationException("Unsupported facing")
    }

val BlockFace.pitch: Float
    get() = when (this) {
        UP -> 90f
        DOWN -> 270f
        else -> 0f
    }

fun BlockFace.getYaw(default: BlockFace): Float =
    (yaw + default.yaw) % 360

val Location.facing: BlockFace
    get() = BlockFaceUtils.getDirection(yaw)

fun Axis.toBlockFace(positive: Boolean): BlockFace =
    when (this) {
        Axis.X -> if (positive) EAST else WEST
        Axis.Y -> if (positive) UP else DOWN
        Axis.Z -> if (positive) SOUTH else NORTH
    }

object BlockFaceUtils {
    
    fun determineBlockFace(block: Block, location: Location): BlockFace {
        val blockMiddle = block.location.add(0.5, 0.5, 0.5)
        val diff = location.clone().subtract(blockMiddle)
        
        val result = listOf(
            Axis.X to diff.x,
            Axis.Z to diff.z,
            Axis.Y to diff.y
        ).sortedByDescending { it.second.absoluteValue }[0]
        
        return result.first.toBlockFace(result.second >= 0)
    }
    
    fun determineBlockFaceLookingAt(location: Location, maxDistance: Double, stepSize: Double): BlockFace? {
        var previous = location
        
        location.castRay(stepSize, maxDistance) { rayLocation ->
            val block = rayLocation.block
            if (block.type.isTraversable() && !block.boundingBox.contains(rayLocation.x, rayLocation.y, rayLocation.z)) {
                previous = rayLocation.clone()
                return@castRay true
            } else {
                return determineBlockFace(rayLocation.block, previous)
            }
        }
        
        return null
    }
    
    fun getDirection(yaw: Float): BlockFace {
        val yawMod = yaw.mod(360f)
        return when {
            yawMod >= 315 -> SOUTH
            yawMod >= 225 -> EAST
            yawMod >= 135 -> NORTH
            yawMod >= 45 -> WEST
            else -> SOUTH
        }
    }
    
}