package xyz.xenondevs.nova.packetentity

import org.bukkit.Location

internal data class ChunkSec(val x: Int, val y: Int, val z: Int)

private data class ChunkSecBox(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
)

internal val PacketEntityLod.minRange: Int
    get() = range.first - 1

internal val PacketEntityLod.maxRange: Int
    get() = range.last

internal val Location.chunkSec: ChunkSec
    get() = ChunkSec(blockX shr 4, blockY shr 4, blockZ shr 4)

internal fun forEachChunkSecInRange(center: ChunkSec, range: Int, run: (ChunkSec) -> Unit) {
    forEachChunkSecInBox(chunkSecBox(center, range), run)
}

internal fun forEachChunkSecInDonut(
    center: ChunkSec,
    minRange: Int,
    maxRange: Int,
    run: (ChunkSec) -> Unit
) {
    if (maxRange <= minRange)
        return
    if (minRange < 0) {
        forEachChunkSecInRange(center, maxRange, run)
    } else {
        forEachChunkSecInRangeDifference(center, maxRange, center, minRange, run)
    }
}

internal fun forEachChunkSecInDonutDifference(
    center: ChunkSec,
    minRange: Int,
    maxRange: Int,
    excludedCenter: ChunkSec,
    excludedMinRange: Int,
    excludedMaxRange: Int,
    run: (ChunkSec) -> Unit
) {
    if (maxRange <= minRange)
        return
    if (excludedMaxRange <= excludedMinRange) {
        forEachChunkSecInDonut(center, minRange, maxRange, run)
        return
    }
    
    val outer = chunkSecBox(center, maxRange)
    val inner = chunkSecBoxOrNull(center, minRange)
    val excludedOuter = chunkSecBox(excludedCenter, excludedMaxRange)
    val excludedInner = chunkSecBoxOrNull(excludedCenter, excludedMinRange)
    
    forEachChunkSecBoxDifference(outer, excludedOuter) { outsideExcludedOuter ->
        forEachChunkSecBoxDifference(outsideExcludedOuter, inner) { result ->
            forEachChunkSecInBox(result, run)
        }
    }
    
    if (excludedInner != null) {
        val overlap = outer.intersection(excludedInner)
        if (overlap != null) {
            forEachChunkSecBoxDifference(overlap, inner) { result ->
                forEachChunkSecInBox(result, run)
            }
        }
    }
}

internal fun ChunkSec.isInRange(center: ChunkSec, range: Int): Boolean =
    maxOf(kotlin.math.abs(x - center.x), kotlin.math.abs(y - center.y), kotlin.math.abs(z - center.z)) <= range

internal fun ChunkSec.isVisibleFrom(center: ChunkSec, lod: PacketEntityLod, playerRange: Int): Boolean {
    val minRange = lod.minRange
    val maxRange = minOf(lod.maxRange, playerRange)
    return maxRange > minRange && isInRange(center, maxRange) && !isInRange(center, minRange)
}

internal fun forEachChunkSecInRangeDifference(
    center: ChunkSec,
    range: Int,
    excludedCenter: ChunkSec,
    excludedRange: Int,
    run: (ChunkSec) -> Unit
) {
    forEachChunkSecBoxDifference(chunkSecBox(center, range), chunkSecBox(excludedCenter, excludedRange)) { result ->
        forEachChunkSecInBox(result, run)
    }
}

private fun chunkSecBox(center: ChunkSec, range: Int) = ChunkSecBox(
    center.x - range, center.x + range,
    center.y - range, center.y + range,
    center.z - range, center.z + range
)

private fun chunkSecBoxOrNull(center: ChunkSec, range: Int): ChunkSecBox? =
    if (range < 0) null else chunkSecBox(center, range)

private fun ChunkSecBox.intersection(other: ChunkSecBox): ChunkSecBox? {
    val intersection = ChunkSecBox(
        maxOf(minX, other.minX), minOf(maxX, other.maxX),
        maxOf(minY, other.minY), minOf(maxY, other.maxY),
        maxOf(minZ, other.minZ), minOf(maxZ, other.maxZ)
    )
    return intersection.takeUnless { it.isEmpty() }
}

private fun ChunkSecBox.isEmpty(): Boolean =
    minX > maxX || minY > maxY || minZ > maxZ

private fun forEachChunkSecBoxDifference(
    box: ChunkSecBox,
    excluded: ChunkSecBox?,
    run: (ChunkSecBox) -> Unit
) {
    val overlap = excluded?.let(box::intersection)
    if (overlap == null) {
        run(box)
        return
    }
    
    run(ChunkSecBox(box.minX, overlap.minX - 1, box.minY, box.maxY, box.minZ, box.maxZ))
    run(ChunkSecBox(overlap.maxX + 1, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ))
    
    run(ChunkSecBox(overlap.minX, overlap.maxX, box.minY, overlap.minY - 1, box.minZ, box.maxZ))
    run(ChunkSecBox(overlap.minX, overlap.maxX, overlap.maxY + 1, box.maxY, box.minZ, box.maxZ))
    
    run(ChunkSecBox(overlap.minX, overlap.maxX, overlap.minY, overlap.maxY, box.minZ, overlap.minZ - 1))
    run(ChunkSecBox(overlap.minX, overlap.maxX, overlap.minY, overlap.maxY, overlap.maxZ + 1, box.maxZ))
}

private fun forEachChunkSecInBox(box: ChunkSecBox, run: (ChunkSec) -> Unit) {
    if (box.isEmpty())
        return
    
    for (x in box.minX..box.maxX) {
        for (y in box.minY..box.maxY) {
            for (z in box.minZ..box.maxZ) {
                run(ChunkSec(x, y, z))
            }
        }
    }
}
