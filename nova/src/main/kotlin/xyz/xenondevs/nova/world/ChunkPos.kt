package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import java.util.*
import kotlin.math.floor

val Chunk.pos: ChunkPos
    get() = ChunkPos(world.uid, x, z)

val Location.chunkPos: ChunkPos
    get() = ChunkPos(world!!.uid, floor(x).toInt() shr 4, floor(z).toInt() shr 4)

val Block.chunkPos: ChunkPos
    get() = ChunkPos(world.uid, x shr 4, z shr 4)

data class ChunkPos(val worldUUID: UUID, val x: Int, val z: Int) {
    
    val chunk: Chunk?
        get() = world?.getChunkAt(x, z)
    
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
    
    fun isLoaded() = world?.isChunkLoaded(x, z) ?: false
    
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