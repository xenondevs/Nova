package xyz.xenondevs.nova.world

import net.minecraft.world.level.block.entity.BlockEntity
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.joml.Vector3i
import xyz.xenondevs.nova.util.Location
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.level.block.state.BlockState as MojangBlockState

val Location.pos: BlockPos
    get() = BlockPos(world!!, blockX, blockY, blockZ)

val Block.pos: BlockPos
    get() = BlockPos(world, x, y, z)

data class BlockPos(val world: World, val x: Int, val y: Int, val z: Int) {
    
    val nmsPos: MojangBlockPos
        get() = MojangBlockPos(x, y, z)
    
    val location: Location
        get() = Location(world, x, y, z)
    
    val vector3i: Vector3i
        get() = Vector3i(x, y, z)
    
    val block: Block
        get() = world.getBlockAt(x, y, z)
    
    val blockState: BlockState
        get() = world.getBlockState(x, y, z)
    
    val novaBlockState: NovaBlockState?
        get() = WorldDataManager.getBlockState(this)
    
    val nmsBlockState: MojangBlockState
        get() = world.serverLevel.getBlockState(nmsPos)
    
    val nmsBlockEntity: BlockEntity?
        get() = world.serverLevel.getBlockEntity(nmsPos)
    
    val chunkPos: ChunkPos
        get() = ChunkPos(world.uid, x shr 4, z shr 4)
    
    val below: BlockPos
        get() = add(0, -1, 0)
    
    fun add(x: Int, y: Int, z: Int): BlockPos =
        BlockPos(world, this.x + x, this.y + y, this.z + z)
    
    fun advance(face: BlockFace, step: Int = 1): BlockPos =
        add(face.modX * step, face.modY * step, face.modZ * step)
    
    fun playSound(sound: String, volume: Float, pitch: Float) {
        world.playSound(Location(world, x + .5, y + .5, z + .5), sound, volume, pitch)
    }
    
    fun playSound(sound: String, category: SoundCategory, volume: Float, pitch: Float) {
        world.playSound(Location(world, x + .5, y + .5, z + .5), sound, category, volume, pitch)
    }
    
    fun playSound(sound: Sound, volume: Float, pitch: Float) {
        world.playSound(Location(world, x + .5, y + .5, z + .5), sound, volume, pitch)
    }
    
}