package xyz.xenondevs.nova.util

import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import xyz.xenondevs.commons.collections.after
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val NESW: List<BlockFace> = listOf(NORTH, EAST, SOUTH, WEST)

/**
 * The side of a block.
 */
enum class BlockSide(private val rotation: Int) {
    
    FRONT(0),
    LEFT(1),
    BACK(2),
    RIGHT(3),
    TOP(-1),
    BOTTOM(-1);
    
    /**
     * Gets the [BlockFace] of this [BlockSide] if [front] is the front direction.
     */
    fun getBlockFace(front: BlockFace): BlockFace {
        require(front in NESW) { "Front must be one of the cardinal directions." }
        return when (this) {
            FRONT -> front
            TOP -> UP
            BOTTOM -> DOWN
            else -> NESW.after(front, rotation)
        }
    }
    
    /**
     * Gets the [BlockFace] of this [BlockSide] for blocks facing the given [yaw].
     */
    fun getBlockFace(yaw: Float): BlockFace =
        when (this) {
            TOP -> UP
            BOTTOM -> DOWN
            else -> {
                val rot = ((yaw / 90.0).roundToInt() + rotation + 2).mod(4)
                NESW[rot]
            }
        }
    
}

/**
 * The axis of a [BlockFace]
 *
 * @throws IllegalArgumentException if the [BlockFace] is not aligned with any axis.
 */
val BlockFace.axis: Axis
    get() = when (this) {
        NORTH -> Axis.Z
        SOUTH -> Axis.Z
        EAST -> Axis.X
        WEST -> Axis.X
        UP -> Axis.Y
        DOWN -> Axis.Y
        else -> throw IllegalArgumentException("BlockFace is not aligned with any axis.")
    }

/**
 * The corresponding yaw value.
 */
val BlockFace.yaw: Float
    get() = when (this) {
        SOUTH -> 0f
        SOUTH_SOUTH_WEST -> 22.5f
        SOUTH_WEST -> 45f
        WEST_SOUTH_WEST -> 67.5f
        WEST -> 90f
        WEST_NORTH_WEST -> 112.5f
        NORTH_WEST -> 135f
        NORTH_NORTH_WEST -> 157.5f
        NORTH -> 180f
        NORTH_NORTH_EAST -> 202.5f
        NORTH_EAST -> 225f
        EAST_NORTH_EAST -> 247.5f
        EAST -> 270f
        EAST_SOUTH_EAST -> 292.5f
        SOUTH_EAST -> 315f
        SOUTH_SOUTH_EAST -> 337.5f
        else -> 0f
    }

/**
 * The corresponding pitch value.
 */
val BlockFace.pitch: Float
    get() = when (this) {
        UP -> -90f
        DOWN -> 90f
        else -> 0f
    }

// TODO: doc
object BlockFaceUtils {
    
    fun determineBlockFace(block: Block, location: Location): BlockFace {
        val result = listOf(
            Axis.X to location.x - (block.x + 0.5),
            Axis.Y to location.y - (block.y + 0.5),
            Axis.Z to location.z - (block.z + 0.5)
        ).sortedByDescending { it.second.absoluteValue }[0]
        
        return toFace(result.first, result.second >= 0)
    }
    
    fun determineBlockFaceLookingAt(location: Location, maxDistance: Double = 6.0): BlockFace? {
        val start = location.vec3
        val direction = location.direction
        val end = start.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance)
        
        val ctx = ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty())
        val result = location.world!!.serverLevel.clip(ctx)
        if (result.type == HitResult.Type.BLOCK) {
            return result.direction.blockFace
        }
        
        return null
    }
    
    fun toFace(axis: Axis, positive: Boolean = true): BlockFace {
        return when (axis) {
            Axis.X -> if (positive) EAST else WEST
            Axis.Y -> if (positive) UP else DOWN
            Axis.Z -> if (positive) SOUTH else NORTH
        }
    }
    
    fun toCartesianFace(yaw: Float): BlockFace {
        val yawMod = yaw.mod(360f)
        return when {
            yawMod >= 315 -> SOUTH
            yawMod >= 225 -> EAST
            yawMod >= 135 -> NORTH
            yawMod >= 45 -> WEST
            else -> SOUTH
        }
    }
    
    fun toCartesianFace(yaw: Float, pitch: Float): BlockFace {
        val yawMod = yaw.mod(360f)
        val pitchMod = pitch.coerceIn(-90f..90f)
        return when {
            pitchMod < -45 -> UP
            pitchMod > 45 -> DOWN
            yawMod >= 315 -> SOUTH
            yawMod >= 225 -> EAST
            yawMod >= 135 -> NORTH
            yawMod >= 45 -> WEST
            else -> SOUTH
        }
    }
    
    fun toRotation(yaw: Float): BlockFace {
        val yawMod = yaw.mod(360f)
        return when {
            yawMod >= 348.75 -> SOUTH
            yawMod >= 326.25 -> SOUTH_SOUTH_EAST
            yawMod >= 303.75 -> SOUTH_EAST
            yawMod >= 281.25 -> EAST_SOUTH_EAST
            yawMod >= 258.75 -> EAST
            yawMod >= 236.25 -> EAST_NORTH_EAST
            yawMod >= 213.75 -> NORTH_EAST
            yawMod >= 191.25 -> NORTH_NORTH_EAST
            yawMod >= 168.75 -> NORTH
            yawMod >= 146.25 -> NORTH_NORTH_WEST
            yawMod >= 123.75 -> NORTH_WEST
            yawMod >= 101.25 -> WEST_NORTH_WEST
            yawMod >= 78.75 -> WEST
            yawMod >= 56.25 -> WEST_SOUTH_WEST
            yawMod >= 33.75 -> SOUTH_WEST
            yawMod >= 11.25 -> SOUTH_SOUTH_WEST
            else -> SOUTH
        }
    }
    
    fun toAxis(yaw: Float): Axis {
        val yawMod = yaw.mod(360f)
        return when {
            yawMod >= 315 -> Axis.Z
            yawMod >= 225 -> Axis.X
            yawMod >= 135 -> Axis.Z
            yawMod >= 45 -> Axis.X
            else -> Axis.Z
        }
    }
    
    fun toAxis(yaw: Float, pitch: Float): Axis {
        val yawMod = yaw.mod(360f)
        val pitchMod = pitch.coerceIn(-90f..90f)
        return when {
            pitchMod < -45 -> Axis.Y
            pitchMod > 45 -> Axis.Y
            yawMod >= 315 -> Axis.Z
            yawMod >= 225 -> Axis.X
            yawMod >= 135 -> Axis.Z
            yawMod >= 45 -> Axis.X
            else -> Axis.Z
        }
    }
    
}