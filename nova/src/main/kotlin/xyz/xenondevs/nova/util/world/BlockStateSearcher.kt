@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util.world

import it.unimi.dsi.fastutil.ints.IntArraySet
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.util.BitStorage
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap
import net.minecraft.util.ZeroBitStorage
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.SingleValuePalette
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HASH_MAP_PALETTE_VALUES_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.LINEAR_PALETTE_VALUES_FIELD
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.BlockLocation
import xyz.xenondevs.nova.world.ChunkPos
import java.util.function.Predicate
import kotlin.reflect.jvm.jvmName

private val ZERO_SET = IntArraySet(intArrayOf(0))
typealias ChunkSearchQuery = Predicate<BlockState>

object BlockStateSearcher {
    
    private val globalPaletteCache = HashMap<ChunkSearchQuery, Set<Int>>()
    
    fun searchChunk(pos: ChunkPos, queries: List<ChunkSearchQuery>): Array<ArrayList<BlockLocation>?> {
        val world = pos.world
        require(world != null) { "World does not exist" }
        
        val result: Array<ArrayList<BlockLocation>?> = arrayOfNulls(queries.size)
        for (section in world.serverLevel.getChunk(pos.x, pos.z).sections) {
            val container = section.states
            container.acquire()
            
            try {
                val bottomY = section.bottomBlockY()
                val data = ReflectionRegistry.PALETTED_CONTAINER_DATA_FIELD.get(container)
                val palette = ReflectionRegistry.PALETTED_CONTAINER_DATA_PALETTE_FIELD.get(data) as Palette<BlockState>
                var storage: BitStorage? = null
                
                for ((queryIdx, query) in queries.withIndex()) {
                    val ids = palette.findIds(query)
                    if (ids.isEmpty())
                        continue
                    
                    if (storage == null)
                        storage = ReflectionRegistry.PALETTED_CONTAINER_DATA_STORAGE_FIELD.get(data) as BitStorage
                    
                    if (storage is ZeroBitStorage)
                        break
                    
                    val resultList = result.getOrSet(queryIdx, ::ArrayList)
                    storage.runOnIds(ids) { idx ->
                        val x = idx and 0xF
                        val z = (idx shr 4) and 0xF
                        val y = idx shr 8
                        
                        resultList += BlockLocation(world, (pos.x shl 4) + x, y + bottomY, (pos.z shl 4) + z)
                    }
                }
            } finally {
                container.release()
            }
        }
        
        return result
    }
    
    private fun Palette<BlockState>.findIds(query: ChunkSearchQuery): Set<Int> {
        return when (this) {
            is SingleValuePalette<BlockState> -> findIdSingle(query)
            is LinearPalette<BlockState> -> findIdsLinear(query)
            is HashMapPalette<BlockState> -> findIdsHashMap(query)
            is GlobalPalette<BlockState> -> findIdsGlobal(query)
            else -> throw UnsupportedOperationException("Unsupported palette type ${this::class.jvmName}")
        }
    }
    
    private fun SingleValuePalette<BlockState>.findIdSingle(query: ChunkSearchQuery): Set<Int> {
        return if (maybeHas(query))
            ZERO_SET
        else emptySet()
    }
    
    private fun LinearPalette<BlockState>.findIdsLinear(query: ChunkSearchQuery): Set<Int> {
        val result = IntArraySet()
        val values = LINEAR_PALETTE_VALUES_FIELD.get(this) as Array<Any?>
        
        for ((idx, value) in values.withIndex()) {
            if (value == null)
                continue
            
            value as BlockState
            if (query.test(value))
                result += idx
        }
        
        return result
    }
    
    private fun HashMapPalette<BlockState>.findIdsHashMap(query: ChunkSearchQuery): Set<Int> {
        val result = IntOpenHashSet()
        val values = HASH_MAP_PALETTE_VALUES_FIELD.get(this) as CrudeIncrementalIntIdentityHashBiMap<BlockState>
        
        for ((idx, value) in values.iterator().withIndex()) {
            if (query.test(value))
                result += idx
        }
        
        return result
    }
    
    private fun findIdsGlobal(query: ChunkSearchQuery): Set<Int> {
        return globalPaletteCache.getOrPut(query) {
            val result = IntOpenHashSet()
            
            for ((idx, value) in Block.BLOCK_STATE_REGISTRY.iterator().withIndex()) {
                if (query.test(value))
                    result += idx
            }
            
            return@getOrPut result
        }
    }
    
    @Suppress("NAME_SHADOWING")
    private inline fun BitStorage.runOnIds(find: Set<Int>, run: (Int) -> Unit) {
        val bits = bits
        val data = raw
        
        val valuesPerLong = 64 / bits
        val mask = (1L shl bits) - 1L
        
        var idx = 0
        
        for (l in data) {
            var l = l
            repeat(valuesPerLong) {
                val id = (l and mask).toInt()
                
                if (id in find)
                    run(idx)
                
                l = l shr bits
                idx++
            }
        }
    }
    
}