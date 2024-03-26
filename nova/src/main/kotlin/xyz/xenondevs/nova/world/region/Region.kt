package xyz.xenondevs.nova.world.region

import org.bukkit.Location
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
import java.util.*

operator fun Location.rangeTo(loc: Location) = Region(this, loc)

class Region(val min: Location, val max: Location) : Iterable<Block> {
    
    val blocks: List<Block> by lazy {
        val blockList = ArrayList<Block>()
        for (x in min.blockX..<max.blockX)
            for (y in min.blockY..<max.blockY)
                for (z in min.blockZ..<max.blockZ)
                    blockList.add(min.world!!.getBlockAt(x, y, z))
        return@lazy Collections.unmodifiableList(blockList)
    }
    
    val world by lazy { min.world }
    
    init {
        require(min.world != null && min.world == max.world) { "Points must be in the same world." }
    }
    
    operator fun contains(loc: Location): Boolean {
        return loc.world == min.world
            && loc.x >= min.x && loc.x <= max.x
            && loc.y >= min.y && loc.y <= max.y
            && loc.z >= min.z && loc.z <= max.z
    }
    
    operator fun contains(block: Block): Boolean {
        val loc = block.location
        return loc.world == min.world
            && loc.x >= min.x && loc.x < max.x
            && loc.y >= min.y && loc.y < max.y
            && loc.z >= min.z && loc.z < max.z
    }
    
    operator fun get(index: Int) = blocks[index]
    
    override fun iterator() = blocks.iterator()
    
    companion object {
        
        fun surrounding(location: Location, radius: Double): Region =
            Region(
                location.clone().subtract(radius, radius, radius),
                location.clone().add(radius, radius, radius)
            )
        
        fun surrounding(pos: BlockPos, radius: Int): Region =
            Region(
                Location(pos.world, pos.x - radius, pos.y - radius, pos.z - radius),
                Location(pos.world, pos.x + radius + 1, pos.y + radius + 1, pos.z + radius + 1)
            )
        
        fun inFrontOf(tileEntity: TileEntity, depth: Double, width: Double, height: Double, translateY: Double): Region {
            val blockState = tileEntity.blockState
            
            val facing = blockState.getOrThrow(DefaultBlockStateProperties.FACING)
            
            return inFrontOf(tileEntity.pos, facing, depth, width, height, translateY)
        }
        
        fun inFrontOf(pos: BlockPos, facing: BlockFace, depth: Double, width: Double, height: Double, translateY: Double): Region {
            val location = pos.location
                .add(0.5, 0.5, 0.5)
                .advance(facing, 0.5)
            
            val direction = facing.direction.toVector3d()
            
            return inDirection(location, direction, depth, width, height, translateY)
        }
        
        fun inDirection(location: Location, direction: Vector3d, depth: Double, width: Double, height: Double, translateY: Double): Region {
            val leftDir = Vector3d(direction).rotateY(-90.0)
            val rightDir = Vector3d(direction).rotateY(90.0)
            
            val pos1 = location.clone().apply {
                add(leftDir.mul(width / 2))
                y += translateY
            }
            
            val pos2 = location.clone().apply {
                add(direction.mul(depth))
                add(rightDir.mul(width / 2))
                add(0.0, translateY + height, 0.0)
            }
            
            val (min, max) = LocationUtils.sort(pos1, pos2)
            return Region(min, max)
        }
        
    }
    
}