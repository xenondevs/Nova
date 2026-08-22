package xyz.xenondevs.nova.packetentity

import org.bukkit.Location

/**
 * Coordinates in one of the spatial grids used for packet-entity tracking.
 */
internal data class TrackingCell(val x: Int, val y: Int, val z: Int)

private data class TrackingCellBox(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
)

internal val PacketEntityVisibility.minRange: Int
    get() = range.first - 1

internal val PacketEntityVisibility.maxRange: Int
    get() = range.last

internal fun Location.trackingCell(visibility: PacketEntityVisibility): TrackingCell =
    TrackingCell(
        blockX shr visibility.cellShift,
        blockY shr visibility.cellShift,
        blockZ shr visibility.cellShift
    )

internal fun forEachTrackingCellInRange(center: TrackingCell, range: Int, run: (TrackingCell) -> Unit) {
    forEachTrackingCellInBox(trackingCellBox(center, range), run)
}

internal fun forEachTrackingCellInDonut(
    center: TrackingCell,
    minRange: Int,
    maxRange: Int,
    run: (TrackingCell) -> Unit
) {
    if (maxRange <= minRange)
        return
    if (minRange < 0) {
        forEachTrackingCellInRange(center, maxRange, run)
    } else {
        forEachTrackingCellInRangeDifference(center, maxRange, center, minRange, run)
    }
}

internal fun forEachTrackingCellInDonutDifference(
    center: TrackingCell,
    minRange: Int,
    maxRange: Int,
    excludedCenter: TrackingCell,
    excludedMinRange: Int,
    excludedMaxRange: Int,
    run: (TrackingCell) -> Unit
) {
    if (maxRange <= minRange)
        return
    if (excludedMaxRange <= excludedMinRange) {
        forEachTrackingCellInDonut(center, minRange, maxRange, run)
        return
    }
    
    val outer = trackingCellBox(center, maxRange)
    val inner = trackingCellBoxOrNull(center, minRange)
    val excludedOuter = trackingCellBox(excludedCenter, excludedMaxRange)
    val excludedInner = trackingCellBoxOrNull(excludedCenter, excludedMinRange)
    
    forEachTrackingCellBoxDifference(outer, excludedOuter) { outsideExcludedOuter ->
        forEachTrackingCellBoxDifference(outsideExcludedOuter, inner) { result ->
            forEachTrackingCellInBox(result, run)
        }
    }
    
    if (excludedInner != null) {
        val overlap = outer.intersection(excludedInner)
        if (overlap != null) {
            forEachTrackingCellBoxDifference(overlap, inner) { result ->
                forEachTrackingCellInBox(result, run)
            }
        }
    }
}

internal fun TrackingCell.isInRange(center: TrackingCell, range: Int): Boolean =
    maxOf(kotlin.math.abs(x - center.x), kotlin.math.abs(y - center.y), kotlin.math.abs(z - center.z)) <= range

internal fun TrackingCell.isVisibleFrom(
    center: TrackingCell,
    visibility: PacketEntityVisibility,
    playerRange: Int
): Boolean {
    val minRange = visibility.minRange
    val maxRange = minOf(visibility.maxRange, playerRange)
    return maxRange > minRange && isInRange(center, maxRange) && !isInRange(center, minRange)
}

internal fun forEachTrackingCellInRangeDifference(
    center: TrackingCell,
    range: Int,
    excludedCenter: TrackingCell,
    excludedRange: Int,
    run: (TrackingCell) -> Unit
) {
    forEachTrackingCellBoxDifference(
        trackingCellBox(center, range),
        trackingCellBox(excludedCenter, excludedRange)
    ) { result ->
        forEachTrackingCellInBox(result, run)
    }
}

private fun trackingCellBox(center: TrackingCell, range: Int) = TrackingCellBox(
    center.x - range, center.x + range,
    center.y - range, center.y + range,
    center.z - range, center.z + range
)

private fun trackingCellBoxOrNull(center: TrackingCell, range: Int): TrackingCellBox? =
    if (range < 0) null else trackingCellBox(center, range)

private fun TrackingCellBox.intersection(other: TrackingCellBox): TrackingCellBox? {
    val intersection = TrackingCellBox(
        maxOf(minX, other.minX), minOf(maxX, other.maxX),
        maxOf(minY, other.minY), minOf(maxY, other.maxY),
        maxOf(minZ, other.minZ), minOf(maxZ, other.maxZ)
    )
    return intersection.takeUnless { it.isEmpty() }
}

private fun TrackingCellBox.isEmpty(): Boolean =
    minX > maxX || minY > maxY || minZ > maxZ

private fun forEachTrackingCellBoxDifference(
    box: TrackingCellBox,
    excluded: TrackingCellBox?,
    run: (TrackingCellBox) -> Unit
) {
    val overlap = excluded?.let(box::intersection)
    if (overlap == null) {
        run(box)
        return
    }
    
    run(TrackingCellBox(box.minX, overlap.minX - 1, box.minY, box.maxY, box.minZ, box.maxZ))
    run(TrackingCellBox(overlap.maxX + 1, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ))
    
    run(TrackingCellBox(overlap.minX, overlap.maxX, box.minY, overlap.minY - 1, box.minZ, box.maxZ))
    run(TrackingCellBox(overlap.minX, overlap.maxX, overlap.maxY + 1, box.maxY, box.minZ, box.maxZ))
    
    run(TrackingCellBox(overlap.minX, overlap.maxX, overlap.minY, overlap.maxY, box.minZ, overlap.minZ - 1))
    run(TrackingCellBox(overlap.minX, overlap.maxX, overlap.minY, overlap.maxY, overlap.maxZ + 1, box.maxZ))
}

private fun forEachTrackingCellInBox(box: TrackingCellBox, run: (TrackingCell) -> Unit) {
    if (box.isEmpty())
        return
    
    for (x in box.minX..box.maxX) {
        for (y in box.minY..box.maxY) {
            for (z in box.minZ..box.maxZ) {
                run(TrackingCell(x, y, z))
            }
        }
    }
}
