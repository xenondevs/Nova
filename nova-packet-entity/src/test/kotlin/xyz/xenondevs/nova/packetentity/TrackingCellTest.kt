package xyz.xenondevs.nova.packetentity

import org.bukkit.Location
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random
import kotlin.test.assertEquals

internal class TrackingCellTest {
    
    @Test
    fun `tracking strategies use their own tracking-cell sizes`() {
        val location = Location(null, 31.9, 17.0, -0.1)
        
        assertEquals(TrackingCell(1, 1, -1), location.trackingCell(PacketEntityVisibility.STANDARD))
        assertEquals(TrackingCell(15, 8, -1), location.trackingCell(PacketEntityVisibility.NEAR))
    }
    
    @ParameterizedTest(name = "{index}: center={0}, range={1}, excludedCenter={2}, excludedRange={3}")
    @MethodSource("rangeDifferenceCases")
    fun `range difference matches naive implementation`(
        center: TrackingCell,
        range: Int,
        excludedCenter: TrackingCell,
        excludedRange: Int
    ) {
        val actual = buildList {
            forEachTrackingCellInRangeDifference(center, range, excludedCenter, excludedRange, ::add)
        }
        val excluded = chunksInRange(excludedCenter, excludedRange)
        val expected = buildSet {
            forEachTrackingCellInRange(center, range) { cell ->
                if (cell !in excluded)
                    add(cell)
            }
        }
        
        assertEquals(actual.toSet().size, actual.size, "Duplicate tracking cells")
        assertEquals(expected, actual.toSet(), "Incorrect tracking cells")
    }
    
    @ParameterizedTest(name = "{index}: visibility={0}, distance={1}, playerRange={2}, visible={3}")
    @CsvSource(
        "NEAR, 1, 16, true",
        "NEAR, 2, 16, false",
        "STANDARD, 8, 8, true",
        "STANDARD, 9, 8, false"
    )
    fun `visibility uses fixed boundaries and player range`(
        visibilityName: String,
        distance: Int,
        playerRange: Int,
        visible: Boolean
    ) {
        val visibility = PacketEntityVisibility.valueOf(visibilityName)
        
        assertEquals(
            visible,
            TrackingCell(distance, 0, 0)
                .isVisibleFrom(TrackingCell(0, 0, 0), visibility, playerRange)
        )
    }
    
    @ParameterizedTest(name = "{index}: center={0}, donut={1}..{2}, excludedCenter={3}, excludedDonut={4}..{5}")
    @MethodSource("donutDifferenceCases")
    fun `donut difference matches naive implementation`(
        center: TrackingCell,
        minRange: Int,
        maxRange: Int,
        excludedCenter: TrackingCell,
        excludedMinRange: Int,
        excludedMaxRange: Int
    ) {
        val actual = buildList {
            forEachTrackingCellInDonutDifference(
                center, minRange, maxRange,
                excludedCenter, excludedMinRange, excludedMaxRange,
                ::add
            )
        }
        val expected = chunksInDonut(center, minRange, maxRange) -
            chunksInDonut(excludedCenter, excludedMinRange, excludedMaxRange)
        
        assertEquals(actual.toSet().size, actual.size, "Duplicate tracking cells")
        assertEquals(expected, actual.toSet(), "Incorrect tracking cells")
    }
    
    private fun chunksInRange(center: TrackingCell, range: Int): Set<TrackingCell> =
        buildSet { forEachTrackingCellInRange(center, range, ::add) }
    
    private fun chunksInDonut(center: TrackingCell, minRange: Int, maxRange: Int): Set<TrackingCell> =
        buildSet { forEachTrackingCellInDonut(center, minRange, maxRange, ::add) }
    
    companion object {
        
        @JvmStatic
        fun rangeDifferenceCases(): List<Array<Any>> = buildList {
            add(arrayOf(TrackingCell(0, 0, 0), 4, TrackingCell(0, 0, 0), 4))
            add(arrayOf(TrackingCell(0, 0, 0), 6, TrackingCell(0, 0, 0), 4))
            add(arrayOf(TrackingCell(0, 0, 0), 2, TrackingCell(0, 0, 0), 4))
            add(arrayOf(TrackingCell(0, 0, 0), 4, TrackingCell(1, 0, 0), 4))
            add(arrayOf(TrackingCell(0, 0, 0), 4, TrackingCell(1, 1, 1), 4))
            add(arrayOf(TrackingCell(0, 0, 0), 4, TrackingCell(20, 20, 20), 2))
            
            val random = Random(42)
            repeat(500) {
                add(
                    arrayOf(
                        TrackingCell(random.nextInt(-20, 21), random.nextInt(-20, 21), random.nextInt(-20, 21)),
                        random.nextInt(0, 6),
                        TrackingCell(random.nextInt(-20, 21), random.nextInt(-20, 21), random.nextInt(-20, 21)),
                        random.nextInt(0, 6)
                    )
                )
            }
        }
        
        @JvmStatic
        fun donutDifferenceCases(): List<Array<Any>> = buildList {
            add(arrayOf(TrackingCell(0, 0, 0), -1, 2, TrackingCell(1, 0, 0), -1, 2))
            add(arrayOf(TrackingCell(0, 0, 0), 2, 4, TrackingCell(1, 0, 0), 2, 4))
            add(arrayOf(TrackingCell(0, 0, 0), 4, 16, TrackingCell(1, 1, 1), 4, 8))
            add(arrayOf(TrackingCell(0, 0, 0), 4, 4, TrackingCell(0, 0, 0), -1, 4))
            add(arrayOf(TrackingCell(0, 0, 0), -1, 8, TrackingCell(0, 0, 0), 4, 16))
            
            val random = Random(43)
            repeat(500) {
                val minRange = random.nextInt(-1, 6)
                val excludedMinRange = random.nextInt(-1, 6)
                add(
                    arrayOf(
                        TrackingCell(random.nextInt(-10, 11), random.nextInt(-10, 11), random.nextInt(-10, 11)),
                        minRange,
                        random.nextInt(minRange + 1, 8),
                        TrackingCell(random.nextInt(-10, 11), random.nextInt(-10, 11), random.nextInt(-10, 11)),
                        excludedMinRange,
                        random.nextInt(excludedMinRange + 1, 8)
                    )
                )
            }
        }
        
    }
    
}
