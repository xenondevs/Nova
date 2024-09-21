@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util.world

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntSet
import net.minecraft.util.BitStorage
import net.minecraft.util.ZeroBitStorage
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.SingleValuePalette
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.nova.patch.impl.worldgen.chunksection.LevelChunkSectionWrapper
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import kotlin.reflect.jvm.jvmName

private typealias ChunkSearchQuery = (BlockState) -> Boolean

object BlockStateSearcher {
    
    private val globalPaletteCache = HashMap<ChunkSearchQuery, Int2ObjectMap<BlockState>>()
    
    fun searchChunk(pos: ChunkPos, queries: List<ChunkSearchQuery>): Array<ArrayList<Pair<BlockPos, BlockState>>?> {
        val world = pos.world
        require(world != null) { "World does not exist" }
        
        val result: Array<ArrayList<Pair<BlockPos, BlockState>>?> = arrayOfNulls(queries.size)
        for (section in world.serverLevel.getChunk(pos.x, pos.z).sections) {
            section as LevelChunkSectionWrapper
            
            val container = section.states
            container.acquire()
            
            try {
                val bottomY = section.bottomBlockY
                val data = container.data
                val palette = data.palette
                var storage = data.storage
                
                if (storage is ZeroBitStorage)
                    continue
                
                for ((queryIdx, query) in queries.withIndex()) {
                    val ids = palette.findIds(query)
                    if (ids.isEmpty())
                        continue
                    
                    val resultList = result.getOrSet(queryIdx, ::ArrayList)
                    storage.runOnIds(ids.keys) { id, encodedPos ->
                        val x = encodedPos and 0xF
                        val z = (encodedPos shr 4) and 0xF
                        val y = encodedPos shr 8
                        
                        resultList += BlockPos(world, (pos.x shl 4) + x, y + bottomY, (pos.z shl 4) + z) to ids.get(id)
                    }
                }
            } finally {
                container.release()
            }
        }
        
        return result
    }
    
    private fun Palette<BlockState>.findIds(query: ChunkSearchQuery): Int2ObjectMap<BlockState> {
        return when (this) {
            is SingleValuePalette<BlockState> -> findIdSingle(query)
            is LinearPalette<BlockState> -> findIdsLinear(query)
            is HashMapPalette<BlockState> -> findIdsHashMap(query)
            is GlobalPalette<BlockState> -> findIdsGlobal(query)
            else -> throw UnsupportedOperationException("Unsupported palette type ${this::class.jvmName}")
        }
    }
    
    private fun SingleValuePalette<BlockState>.findIdSingle(query: ChunkSearchQuery): Int2ObjectMap<BlockState> {
        if (maybeHas(query)) {
            return Int2ObjectArrayMap(intArrayOf(0), arrayOf(valueFor(0)))
        } else {
            return Int2ObjectArrayMap(intArrayOf(), arrayOf())
        }
    }
    
    private fun LinearPalette<BlockState>.findIdsLinear(query: ChunkSearchQuery): Int2ObjectMap<BlockState> {
        val result = Int2ObjectOpenHashMap<BlockState>()
        
        for ((idx, value) in values.withIndex()) {
            if (value == null)
                continue
            
            if (query(value)) {
                result.put(idx, value)
            }
        }
        
        return result
    }
    
    private fun HashMapPalette<BlockState>.findIdsHashMap(query: ChunkSearchQuery): Int2ObjectMap<BlockState> {
        val result = Int2ObjectOpenHashMap<BlockState>()
        
        for ((idx, value) in values.withIndex()) {
            if (query(value)) {
                result.put(idx, value)
            }
        }
        
        return result
    }
    
    private fun findIdsGlobal(query: ChunkSearchQuery): Int2ObjectMap<BlockState> {
        return globalPaletteCache.getOrPut(query) {
            val result = Int2ObjectOpenHashMap<BlockState>()
            
            for ((idx, value) in Block.BLOCK_STATE_REGISTRY.withIndex()) {
                if (query(value)) {
                    result.put(idx, value)
                }
            }
            
            return@getOrPut result
        }
    }
    
    @Suppress("NAME_SHADOWING")
    private inline fun BitStorage.runOnIds(find: IntSet, run: (id: Int, pos: Int) -> Unit) {
        val bits = bits
        val data = raw
        
        val valuesPerLong = 64 / bits
        val mask = (1L shl bits) - 1L
        
        var idx = 0
        
        for (l in data) {
            var l = l
            repeat(valuesPerLong) {
                val id = (l and mask).toInt()
                
                if (find.contains(id))
                    run(id, idx)
                
                l = l shr bits
                idx++
            }
        }
    }
    
}