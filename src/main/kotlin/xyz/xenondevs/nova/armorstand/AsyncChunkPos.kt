package xyz.xenondevs.nova.armorstand

import java.util.*

data class AsyncChunkPos(val world: UUID, val x: Int, val z: Int) {
    
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
    
}