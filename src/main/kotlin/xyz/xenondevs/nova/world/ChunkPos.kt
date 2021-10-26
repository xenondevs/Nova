package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import java.util.*

data class ChunkPos(val world: UUID, val x: Int, val z: Int) {
    
    val chunk: Chunk
        get() = Bukkit.getWorld(world)!!.getChunkAt(x, z)
    
    fun getInRange(range: Int): Set<ChunkPos> {
        val length = 2 * range + 1
        val chunks = HashSet<ChunkPos>(length * length)
        
        for (newX in (x - range)..(x + range)) {
            for (newZ in (z - range)..(z + range)) {
                chunks.add(ChunkPos(world, newX, newZ))
            }
        }
        
        return chunks
    }
    
    fun isLoaded() = Bukkit.getWorld(world)?.isChunkLoaded(x, z) ?: false
    
    override fun equals(other: Any?): Boolean {
        return this === other || (other is ChunkPos && other.world == world && other.x == x && other.z == z)
    }
    
    override fun hashCode(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + z
        return result
    }
    
}