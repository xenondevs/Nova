package xyz.xenondevs.nova.util

import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import org.joml.Quaternionf
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class BlockSide(private val rotation: Int, val blockFace: BlockFace) {
    
    FRONT(0, SOUTH),
    LEFT(1, WEST),
    BACK(2, NORTH),
    RIGHT(3, EAST),
    TOP(-1, UP),
    BOTTOM(-1, DOWN);
    
    fun getBlockFace(yaw: Float): BlockFace {
        if (rotation == -1) return blockFace
        
        val rot = ((yaw / 90.0).roundToInt() + rotation).mod(4)
        return entries[rot].blockFace
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

/**
 * The rotation that needs to be applied to make something face the given [BlockFace], assuming it is facing SOUTH by default.
 */
val BlockFace.rotation: Quaternionf
    get() = when (this) {
        SOUTH -> Quaternionf()
        EAST -> Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 0f, 1f, 0f)
        NORTH -> Quaternionf().setAngleAxis(Math.PI.toFloat(), 0f, 1f, 0f)
        WEST -> Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 0f, 1f, 0f)
        UP -> Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 1f, 0f, 0f)
        DOWN -> Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 1f, 0f, 0f)
        else -> throw UnsupportedOperationException("Unsupported facing")
    }

/**
 * The rotation that needs to be applied to make something face the given [BlockFace], assuming
 * it is facing NORTH by default.
 */
val BlockFace.rotationNorth: Quaternionf
    get() = when (this) {
        SOUTH -> Quaternionf().setAngleAxis(Math.PI.toFloat(), 0f, 1f, 0f)
        EAST -> Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 0f, 1f, 0f)
        NORTH -> Quaternionf()
        WEST -> Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 0f, 1f, 0f)
        UP -> Quaternionf().setAngleAxis((Math.PI / 2).toFloat(), 1f, 0f, 0f)
        DOWN -> Quaternionf().setAngleAxis((Math.PI * 1.5).toFloat(), 1f, 0f, 0f)
        else -> throw UnsupportedOperationException("Unsupported facing")
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
        val result = listOf(
            Axis.X to location.x - (block.x + 0.5),
            Axis.Y to location.y - (block.y + 0.5),
            Axis.Z to location.z - (block.z + 0.5)
        ).sortedByDescending { it.second.absoluteValue }[0]
        
        return result.first.toBlockFace(result.second >= 0)
    }
    
    fun determineBlockFaceLookingAt(location: Location, maxDistance: Double = 6.0): BlockFace? {
        val start = location.vec3
        val direction = location.direction
        val end = start.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance)
        
        val ctx = ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null)
        val result = location.world!!.serverLevel.clip(ctx)
        if (result.type == HitResult.Type.BLOCK) {
            return result.direction.blockFace
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