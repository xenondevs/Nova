package xyz.xenondevs.nova.world.armorstand

import org.bukkit.Bukkit
import org.bukkit.Chunk
import java.util.*

data class AsyncChunkPos(val world: UUID, val x: Int, val z: Int) {
    
    val chunk: Chunk
        get() = Bukkit.getWorld(world)!!.getChunkAt(x, z)
    
    fun getInRange(range: Int): Set<AsyncChunkPos> {
        val length = 2 * range + 1
        val chunks = HashSet<AsyncChunkPos>(length * length)
        
        for (newX in (x - range)..(x + range)) {
            for (newZ in (z - range)..(z + range)) {
                chunks.add(AsyncChunkPos(world, newX, newZ))
            }
        }
        
        return chunks
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || (other is AsyncChunkPos && other.world == world && other.x == x && other.z == z)
    }
    
    override fun hashCode(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + z
        return result
    }
    
}