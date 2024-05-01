package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.joml.Vector3d
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.Location
import xyz.xenondevs.nova.util.LocationUtils
import xyz.xenondevs.nova.util.add
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties

/**
 * Creates a [Region] from this [Location] to the given [Location].
 */
operator fun Location.rangeTo(loc: Location) = Region(this, loc)

/**
 * A region in the world.
 * 
 * @param min The start of the region, inclusive.
 * @param max The end of the region, inclusive.
 */
class Region(val min: Location, val max: Location) {
    
    /**
     * The [World] that this [Region] is in.
     */
    val world: World by lazy { min.world }
    
    init {
        require(min.world != null && min.world == max.world) { "Points must be in the same world." }
    }
    
    /**
     * Checks whether the given [Location] is inside this [Region].
     */
    operator fun contains(loc: Location): Boolean {
        return loc.world == min.world
            && loc.x >= min.x && loc.x <= max.x
            && loc.y >= min.y && loc.y <= max.y
            && loc.z >= min.z && loc.z <= max.z
    }
    
    /**
     * Checks whether the given [Block] is inside this [Region].
     */
    operator fun contains(block: Block): Boolean {
        val loc = block.location
        return loc.world == min.world
            && loc.x >= min.x && loc.x < max.x
            && loc.y >= min.y && loc.y < max.y
            && loc.z >= min.z && loc.z < max.z
    }
    
    companion object {
        
        /**
         * Creates a [Region] surrounding [location] using the given cubic [radius].
         */
        fun surrounding(location: Location, radius: Number): Region {
            val radiusD = radius.toDouble()
            return Region(
                location.clone().subtract(radiusD, radiusD, radiusD),
                location.clone().add(radiusD, radiusD, radiusD)
            )
        }
        
        /**
         * Creates a [Region] surrounding [pos] using the given cubic [radius].
         */
        fun surrounding(pos: BlockPos, radius: Int): Region =
            Region(
                Location(pos.world, pos.x - radius, pos.y - radius, pos.z - radius),
                Location(pos.world, pos.x + radius + 1, pos.y + radius + 1, pos.z + radius + 1)
            )
        
        /**
         * Creates a region in front of [tileEntity] with the given [depth], [width], [height] and [translateY].
         */
        fun inFrontOf(tileEntity: TileEntity, depth: Number, width: Number, height: Number, translateY: Number): Region {
            val blockState = tileEntity.blockState
            val facing = blockState.getOrThrow(DefaultBlockStateProperties.FACING)
            return inFrontOf(tileEntity.pos, facing, depth, width, height, translateY)
        }
        
        /**
         * Creates a region in front of [pos] with the given [depth], [width], [height] and [translateY].
         */
        fun inFrontOf(pos: BlockPos, facing: BlockFace, depth: Number, width: Number, height: Number, translateY: Number): Region {
            val location = pos.location
                .add(0.5, 0.5, 0.5)
                .advance(facing, 0.5)
            val direction = facing.direction.toVector3d()
            return inDirection(location, direction, depth, width, height, translateY)
        }
        
        /**
         * Creates a region in the given [direction] from [location] with the given [depth], [width], [height] and [translateY].
         */
        fun inDirection(location: Location, direction: Vector3d, depth: Number, width: Number, height: Number, translateY: Number): Region {
            val leftDir = Vector3d(direction).rotateY(-90.0)
            val rightDir = Vector3d(direction).rotateY(90.0)
            
            val pos1 = location.clone().apply {
                add(leftDir.mul(width.toDouble() / 2))
                y += translateY.toDouble()
            }
            
            val pos2 = location.clone().apply {
                add(direction.mul(depth.toDouble()))
                add(rightDir.mul(width.toDouble() / 2))
                add(0.0, translateY.toDouble() + height.toDouble(), 0.0)
            }
            
            val (min, max) = LocationUtils.sort(pos1, pos2)
            return Region(min, max)
        }
        
    }
    
}