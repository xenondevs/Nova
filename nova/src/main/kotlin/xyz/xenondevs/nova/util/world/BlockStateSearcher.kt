package xyz.xenondevs.nova.util.world

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.Palette
import org.bukkit.Chunk
import org.bukkit.block.Block
import xyz.xenondevs.nova.util.serverLevel
import java.util.*

object BlockStateSearcher {
    
    private val globalPaletteCache = IdentityHashMap<(BlockState) -> Boolean, Int2ObjectMap<BlockState>>()
    
    /**
     * Runs [onMatch] for each block in [chunk] that matches [query].
     */
    fun searchChunk(chunk: Chunk, query: (BlockState) -> Boolean, onMatch: (Block, BlockState) -> Unit) {
        val nmsChunk = chunk.world.serverLevel.getChunk(chunk.x, chunk.z)
        
        for ([sectionIndex, section] in nmsChunk.sections.withIndex()) {
            val states = section.states
            states.acquire()
            try {
                val data = states.data
                val palette = data.palette()
                val matchingStates = palette.findIds(query)
                if (matchingStates.isEmpty())
                    continue
                
                val storage = data.storage()
                val bottomY = nmsChunk.getSectionYFromSectionIndex(sectionIndex) shl 4
                for (encodedPos in 0..<storage.size) {
                    val state = matchingStates.get(storage.get(encodedPos)) ?: continue
                    val x = (chunk.x shl 4) + (encodedPos and 15)
                    val y = bottomY + (encodedPos shr 8)
                    val z = (chunk.z shl 4) + ((encodedPos shr 4) and 15)
                    onMatch(chunk.world.getBlockAt(x, y, z), state)
                }
            } finally {
                states.release()
            }
        }
    }
    
    private fun Palette<BlockState>.findIds(query: (BlockState) -> Boolean): Int2ObjectMap<BlockState> {
        if (this is GlobalPalette)
            return globalPaletteCache.getOrPut(query) { collectIds(query) }
        return collectIds(query)
    }
    
    private fun Palette<BlockState>.collectIds(query: (BlockState) -> Boolean): Int2ObjectMap<BlockState> {
        val result = Int2ObjectOpenHashMap<BlockState>()
        for (id in 0..<size) {
            val state = valueFor(id)
            if (query(state))
                result.put(id, state)
        }
        return result
    }
    
}
