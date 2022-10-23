package xyz.xenondevs.nova.world

import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.block.Block
import xyz.xenondevs.nova.util.Location

val Location.pos: BlockPos
    get() = BlockPos(world!!, blockX, blockY, blockZ)

val Block.pos: BlockPos
    get() = BlockPos(world, x, y, z)

data class BlockPos(val world: World, val x: Int, val y: Int, val z: Int) {
    
    val location: Location
        get() = Location(world, x, y, z)
    
    val block: Block
        get() = world.getBlockAt(x, y, z)
    
    val chunkPos: ChunkPos
        get() = ChunkPos(world.uid, x shr 4, z shr 4)
    
    val below: BlockPos
        get() = add(0, -1, 0)
    
    fun add(x: Int, y: Int, z: Int): BlockPos =
        BlockPos(world, this.x + x, this.y + y, this.z + z)
    
    fun playSound(sound: String, volume: Float, pitch: Float) {
        world.playSound(Location(world, x + .5, y + .5, z + .5), sound, volume, pitch)
    }
    
    fun playSound(sound: String, category: SoundCategory, volume: Float, pitch: Float) {
        world.playSound(Location(world, x + .5, y + .5, z + .5), sound, category, volume, pitch)
    }
    
}