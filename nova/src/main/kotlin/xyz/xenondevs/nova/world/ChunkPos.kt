package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import java.util.*
import kotlin.math.floor

/**
 * The [ChunkPos] of this [Chunk].
 */
val Chunk.pos: ChunkPos
    get() = ChunkPos(world.uid, x, z)

/**
 * The [ChunkPos] of the [Chunk] at this [Location].
 */
val Location.chunkPos: ChunkPos
    get() = ChunkPos(world!!.uid, floor(x).toInt() shr 4, floor(z).toInt() shr 4)

/**
 * The [ChunkPos] of the [Block] at this position.
 */
val Block.chunkPos: ChunkPos
    get() = ChunkPos(world.uid, x shr 4, z shr 4)

/**
 * A position of a chunk.
 * 
 * @param worldUUID The [UUID] of the world this chunk is in.
 * @param x The x coordinate of the chunk.
 * @param z The z coordinate of the chunk.
 */
data class ChunkPos(val worldUUID: UUID, val x: Int, val z: Int) {
    
    /**
     * The [Chunk] at this [ChunkPos], may be null if [world] is null.
     */
    val chunk: Chunk?
        get() = world?.getChunkAt(x, z)
    
    /**
     * The world of this [ChunkPos], may be null if no world with [worldUUID] exists.
     */
    val world: World?
        get() = Bukkit.getWorld(worldUUID)
    
    fun getInRange(range: Int): Set<ChunkPos> {
        val length = 2 * range + 1
        val chunks = HashSet<ChunkPos>(length * length)
        
        for (newX in (x - range)..(x + range)) {
            for (newZ in (z - range)..(z + range)) {
                chunks.add(ChunkPos(worldUUID, newX, newZ))
            }
        }
        
        return chunks
    }
    
    /**
     * Gets the [BlockPos] at the specified chunk coordinates [x], [y], [z].
     */
    fun blockPos(x: Int, y: Int, z: Int): BlockPos =
        BlockPos(world!!, (this.x shl 4) + x, y, (this.z shl 4) + z)
    
    /**
     * Checks whether the chunk at this position is loaded.
     */
    fun isLoaded(): Boolean =
        world?.isChunkLoaded(x, z) ?: false
    
    /**
     * Converts the chunk position to a long, where the 32 most significant bits are the [z]
     * coordinate and the 32 least significant bits are the [x] coordinate.
     * 
     * This format matches that of [net.minecraft.world.level.ChunkPos.toLong].
     */
    fun toLong(): Long =
        ((z.toLong() and 0xFFFFFFFF) shl 32) or (x.toLong() and 0xFFFFFFFF)
    
    override fun equals(other: Any?): Boolean {
        return this === other || (other is ChunkPos && other.worldUUID == worldUUID && other.x == x && other.z == z)
    }
    
    override fun hashCode(): Int {
        var result = worldUUID.hashCode()
        result = 31 * result + x
        result = 31 * result + z
        return result
    }
    
}