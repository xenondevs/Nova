package xyz.xenondevs.nova.util

import net.minecraft.util.BitStorage
import net.minecraft.util.SimpleBitStorage
import net.minecraft.util.ZeroBitStorage
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.SingleValuePalette
import org.bukkit.World
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.collections.identityHashSet
import java.util.*

/**
 * Matches rectangular world regions against [accepted].
 */
class BlockStateMatcher(accepted: Collection<BlockData>) {
    
    constructor(vararg accepted: BlockData) : this(accepted.asList())
    
    private val acceptedBlockStates = identityHashSet<BlockState>()
    private val globalMatches = BooleanArray(Block.BLOCK_STATE_REGISTRY.size())
    
    init {
        for (blockData in accepted) {
            val state = blockData.nmsBlockState
            acceptedBlockStates.add(state)
            globalMatches[Block.BLOCK_STATE_REGISTRY.getId(state)] = true
        }
    }
    
    /**
     * Matches the rectangular region starting at [x], [y], [z]. Coordinates passed to the returned result are
     * local to that origin.
     *
     * This synchronously accesses (and may load) every intersecting chunk and must run on the owning region thread.
     */
    fun match(
        world: World,
        x: Int, y: Int, z: Int,
        width: Int, height: Int, depth: Int
    ): BlockStateMatchResult {
        val level = world.serverLevel
        val minSectionY = level.minSectionY
        val maxSectionY = level.maxSectionY
        var cachedChunkX = Int.MIN_VALUE
        var cachedChunkZ = Int.MIN_VALUE
        var cachedChunk: LevelChunk? = null
        return match(x, y, z, width, height, depth) { sectionX, sectionY, sectionZ ->
            if (sectionY !in minSectionY..maxSectionY) {
                null
            } else {
                if (sectionX != cachedChunkX || sectionZ != cachedChunkZ) {
                    cachedChunkX = sectionX
                    cachedChunkZ = sectionZ
                    cachedChunk = level.getChunk(sectionX, sectionZ)
                }
                cachedChunk!!.sections[level.getSectionIndexFromSectionY(sectionY)].states.data
            }
        }
    }
    
    internal fun match(
        x: Int, y: Int, z: Int,
        width: Int, height: Int, depth: Int,
        getSectionData: (sectionX: Int, sectionY: Int, sectionZ: Int) -> PalettedContainer.Data<BlockState>?
    ): BlockStateMatchResult {
        require(width >= 0 && height >= 0 && depth >= 0) { "Match dimensions must not be negative" }
        val maxX = Math.addExact(x, width)
        val maxY = Math.addExact(y, height)
        val maxZ = Math.addExact(z, depth)
        val yStride = Math.addExact(width, 1)
        val zStride = Math.multiplyExact(yStride, Math.addExact(height, 1))
        val data = IntArray(Math.multiplyExact(zStride, Math.addExact(depth, 1)))
        
        if (width != 0 && height != 0 && depth != 0) {
            for (sectionZ in (z shr 4)..((maxZ - 1) shr 4)) {
                val sectionBaseZ = sectionZ shl 4
                val fromZ = maxOf(z, sectionBaseZ)
                val toZ = minOf(maxZ, sectionBaseZ + 16)
                for (sectionX in (x shr 4)..((maxX - 1) shr 4)) {
                    val sectionBaseX = sectionX shl 4
                    val fromX = maxOf(x, sectionBaseX)
                    val toX = minOf(maxX, sectionBaseX + 16)
                    for (sectionY in (y shr 4)..((maxY - 1) shr 4)) {
                        val packedSectionData = getSectionData(sectionX, sectionY, sectionZ) ?: continue
                        val sectionBaseY = sectionY shl 4
                        val fromY = maxOf(y, sectionBaseY)
                        val toY = minOf(maxY, sectionBaseY + 16)
                        val destinationOffset =
                            1 + (fromX - x) +
                                (1 + fromY - y) * yStride +
                                (1 + fromZ - z) * zStride
                        copyMatches(
                            packedSectionData,
                            fromX - sectionBaseX, fromY - sectionBaseY, fromZ - sectionBaseZ,
                            toX - sectionBaseX, toY - sectionBaseY, toZ - sectionBaseZ,
                            data, destinationOffset, yStride, zStride
                        )
                    }
                }
            }
            buildPrefixSum(data, width, height, depth, yStride, zStride)
        }
        
        return BlockStateMatchResult(width, height, depth, data)
    }
    
    internal fun copyMatches(
        data: PalettedContainer.Data<BlockState>,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
        destination: IntArray,
        destinationOffset: Int,
        destinationYStride: Int, destinationZStride: Int
    ) {
        validateBounds(
            minX, minY, minZ,
            maxX, maxY, maxZ,
            destination, destinationOffset, destinationYStride, destinationZStride
        )
        if (minX == maxX || minY == maxY || minZ == maxZ)
            return
        
        val paletteMatches = createPaletteMatches(data.palette())
        when {
            !paletteMatches.any -> fill(
                minY, minZ, maxX - minX, maxY, maxZ,
                destination, destinationOffset, destinationYStride, destinationZStride,
                0
            )
            
            paletteMatches.all -> fill(
                minY, minZ, maxX - minX, maxY, maxZ,
                destination, destinationOffset, destinationYStride, destinationZStride,
                1
            )
            
            data.storage() is ZeroBitStorage -> fill(
                minY, minZ, maxX - minX, maxY, maxZ,
                destination, destinationOffset, destinationYStride, destinationZStride,
                if (paletteMatches[0]) 1 else 0
            )
            
            data.storage() is SimpleBitStorage -> copySimpleBitStorage(
                data.storage() as SimpleBitStorage, paletteMatches,
                minX, minY, minZ, maxX, maxY, maxZ,
                destination, destinationOffset, destinationYStride, destinationZStride
            )
            
            else -> copyBitStorage(
                data.storage(), paletteMatches,
                minX, minY, minZ, maxX, maxY, maxZ,
                destination, destinationOffset, destinationYStride, destinationZStride
            )
        }
    }
    
    private fun createPaletteMatches(palette: Palette<BlockState>): PaletteMatches {
        if (acceptedBlockStates.isEmpty())
            return PaletteMatches.NONE
        
        return when (palette) {
            is SingleValuePalette -> {
                val matches = acceptedBlockStates.contains(palette.valueFor(0))
                PaletteMatches(booleanArrayOf(matches), matches, matches)
            }
            
            is LinearPalette -> createLinearPaletteMatches(palette)
            is HashMapPalette -> createLocalPaletteMatches(palette.size) { palette.values.byId(it)!! }
            is GlobalPalette -> PaletteMatches(globalMatches, any = true, all = false)
            else -> createLocalPaletteMatches(palette.size, palette::valueFor)
        }
    }
    
    private fun createLinearPaletteMatches(palette: LinearPalette<*>): PaletteMatches {
        val values = palette.values
        val matches = BooleanArray(palette.size)
        var matchCount = 0
        for (id in 0..<palette.size) {
            if (acceptedBlockStates.contains(values[id])) {
                matches[id] = true
                matchCount++
            }
        }
        return PaletteMatches(matches, matchCount > 0, matchCount == matches.size)
    }
    
    private inline fun createLocalPaletteMatches(size: Int, valueFor: (Int) -> BlockState): PaletteMatches {
        val matches = BooleanArray(size)
        var matchCount = 0
        for (id in 0..<size) {
            if (acceptedBlockStates.contains(valueFor(id))) {
                matches[id] = true
                matchCount++
            }
        }
        return PaletteMatches(matches, matchCount > 0, matchCount == size)
    }
    
    private fun copySimpleBitStorage(
        storage: SimpleBitStorage,
        paletteMatches: PaletteMatches,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
        destination: IntArray,
        destinationOffset: Int,
        destinationYStride: Int, destinationZStride: Int
    ) {
        val bits = storage.bits
        val valuesPerLong = 64 / bits
        val valueMask = (1L shl bits) - 1L
        val raw = storage.raw
        val width = maxX - minX
        
        for (y in minY..<maxY) {
            val destinationY = destinationOffset + (y - minY) * destinationYStride
            for (z in minZ..<maxZ) {
                val sourceIndex = (y shl 8) or (z shl 4) or minX
                var cellIndex = sourceIndex / valuesPerLong
                var indexInCell = sourceIndex - cellIndex * valuesPerLong
                var cell = raw[cellIndex] ushr (indexInCell * bits)
                var destinationIndex = destinationY + (z - minZ) * destinationZStride
                
                repeat(width) { xOffset ->
                    val paletteId = (cell and valueMask).toInt()
                    destination[destinationIndex++] = if (paletteMatches[paletteId]) 1 else 0
                    
                    indexInCell++
                    if (indexInCell == valuesPerLong && xOffset + 1 < width) {
                        indexInCell = 0
                        cell = raw[++cellIndex]
                    } else {
                        cell = cell ushr bits
                    }
                }
            }
        }
    }
    
    private fun copyBitStorage(
        storage: BitStorage,
        paletteMatches: PaletteMatches,
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
        destination: IntArray,
        destinationOffset: Int,
        destinationYStride: Int, destinationZStride: Int
    ) {
        for (y in minY..<maxY) {
            val destinationY = destinationOffset + (y - minY) * destinationYStride
            for (z in minZ..<maxZ) {
                var sourceIndex = (y shl 8) or (z shl 4) or minX
                var destinationIndex = destinationY + (z - minZ) * destinationZStride
                repeat(maxX - minX) {
                    destination[destinationIndex++] = if (paletteMatches[storage[sourceIndex++]]) 1 else 0
                }
            }
        }
    }
    
    private fun fill(
        minY: Int, minZ: Int,
        width: Int,
        maxY: Int, maxZ: Int,
        destination: IntArray,
        destinationOffset: Int,
        destinationYStride: Int, destinationZStride: Int,
        value: Int
    ) {
        for (y in minY..<maxY) {
            val destinationY = destinationOffset + (y - minY) * destinationYStride
            for (z in minZ..<maxZ) {
                val from = destinationY + (z - minZ) * destinationZStride
                Arrays.fill(destination, from, from + width, value)
            }
        }
    }
    
    private fun validateBounds(
        minX: Int, minY: Int, minZ: Int,
        maxX: Int, maxY: Int, maxZ: Int,
        destination: IntArray,
        destinationOffset: Int,
        destinationYStride: Int, destinationZStride: Int
    ) {
        require(minX in 0..16 && minY in 0..16 && minZ in 0..16)
        require(maxX in minX..16 && maxY in minY..16 && maxZ in minZ..16)
        require(destinationOffset >= 0)
        require(destinationYStride > 0 && destinationZStride > 0)
        
        if (minX != maxX && minY != maxY && minZ != maxZ) {
            val lastIndex = destinationOffset +
                (maxX - minX - 1) +
                (maxY - minY - 1) * destinationYStride +
                (maxZ - minZ - 1) * destinationZStride
            require(lastIndex < destination.size)
        }
    }
    
    private fun buildPrefixSum(data: IntArray, width: Int, height: Int, depth: Int, yStride: Int, zStride: Int) {
        for (z in 1..depth) {
            for (y in 1..height) {
                var i = 1 + y * yStride + z * zStride
                repeat(width) {
                    data[i] +=
                        data[i - 1] + data[i - yStride] + data[i - zStride] -
                            data[i - 1 - yStride] - data[i - 1 - zStride] - data[i - yStride - zStride] +
                            data[i - 1 - yStride - zStride]
                    i++
                }
            }
        }
    }
    
    private class PaletteMatches(
        private val matches: BooleanArray,
        val any: Boolean,
        val all: Boolean
    ) {
        operator fun get(id: Int): Boolean = id < matches.size && matches[id]
        
        companion object {
            val NONE = PaletteMatches(BooleanArray(0), any = false, all = false)
        }
    }
    
}

/**
 * Prefix-summed result of matching a rectangular block volume.
 */
class BlockStateMatchResult internal constructor(
    val width: Int,
    val height: Int,
    val depth: Int,
    private val data: IntArray
) {
    
    private val yStride = width + 1
    private val zStride = yStride * (height + 1)
    
    /** Returns whether the block at the given local coordinates matched. */
    operator fun get(x: Int, y: Int, z: Int): Boolean = count(x, y, z, 1, 1, 1) == 1
    
    /** Returns the number of matching blocks in the given local rectangular box. */
    fun count(x: Int, y: Int, z: Int, width: Int, height: Int, depth: Int): Int {
        requireBox(x, y, z, width, height, depth)
        val x1 = x + width
        val y1 = y + height
        val z1 = z + depth
        return data[x1 + y1 * yStride + z1 * zStride] -
            data[x + y1 * yStride + z1 * zStride] -
            data[x1 + y * yStride + z1 * zStride] -
            data[x1 + y1 * yStride + z * zStride] +
            data[x + y * yStride + z1 * zStride] +
            data[x + y1 * yStride + z * zStride] +
            data[x1 + y * yStride + z * zStride] -
            data[x + y * yStride + z * zStride]
    }
    
    /** Returns whether every block in the given local rectangular box matched. */
    fun matchesAll(x: Int, y: Int, z: Int, width: Int, height: Int, depth: Int): Boolean {
        return count(x, y, z, width, height, depth).toLong() ==
            width.toLong() * height.toLong() * depth.toLong()
    }
    
    private fun requireBox(x: Int, y: Int, z: Int, width: Int, height: Int, depth: Int) {
        require(width >= 0 && height >= 0 && depth >= 0) { "Box dimensions must not be negative" }
        require(x >= 0 && y >= 0 && z >= 0) { "Box coordinates must not be negative" }
        require(x <= this.width - width && y <= this.height - height && z <= this.depth - depth) {
            "Box must be contained in the match result"
        }
    }
    
}
