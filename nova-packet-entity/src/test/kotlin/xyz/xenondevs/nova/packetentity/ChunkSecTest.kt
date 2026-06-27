package xyz.xenondevs.nova.packetentity

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random
import kotlin.test.assertEquals

internal class ChunkSecTest {
    
    @ParameterizedTest(name = "{index}: center={0}, range={1}, excludedCenter={2}, excludedRange={3}")
    @MethodSource("rangeDifferenceCases")
    fun `range difference matches naive implementation`(
        center: ChunkSec,
        range: Int,
        excludedCenter: ChunkSec,
        excludedRange: Int
    ) {
        val actual = buildList {
            forEachChunkSecInRangeDifference(center, range, excludedCenter, excludedRange, ::add)
        }
        val excluded = chunksInRange(excludedCenter, excludedRange)
        val expected = buildSet {
            forEachChunkSecInRange(center, range) { chunk ->
                if (chunk !in excluded)
                    add(chunk)
            }
        }
        
        assertEquals(actual.toSet().size, actual.size, "Duplicate chunks")
        assertEquals(expected, actual.toSet(), "Incorrect chunks")
    }

    @ParameterizedTest(name = "{index}: lod={0}, distance={1}, playerRange={2}, visible={3}")
    @CsvSource(
        "NEAR, 1, 16, true",
        "NEAR, 2, 16, false",
        "FAR, 1, 16, false",
        "FAR, 2, 16, true",
        "FAR, 16, 16, true",
        "FAR, 17, 16, false",
        "FAR, 5, 4, false",
        "ALL, 8, 8, true",
        "ALL, 9, 8, false"
    )
    fun `lod visibility uses fixed boundaries and player range`(
        lodName: String,
        distance: Int,
        playerRange: Int,
        visible: Boolean
    ) {
        val lod = PacketEntityLod.valueOf(lodName)
        
        assertEquals(visible, ChunkSec(distance, 0, 0).isVisibleFrom(ChunkSec(0, 0, 0), lod, playerRange))
    }

    @ParameterizedTest(name = "{index}: center={0}, donut={1}..{2}, excludedCenter={3}, excludedDonut={4}..{5}")
    @MethodSource("donutDifferenceCases")
    fun `donut difference matches naive implementation`(
        center: ChunkSec,
        minRange: Int,
        maxRange: Int,
        excludedCenter: ChunkSec,
        excludedMinRange: Int,
        excludedMaxRange: Int
    ) {
        val actual = buildList {
            forEachChunkSecInDonutDifference(
                center, minRange, maxRange,
                excludedCenter, excludedMinRange, excludedMaxRange,
                ::add
            )
        }
        val expected = chunksInDonut(center, minRange, maxRange) -
            chunksInDonut(excludedCenter, excludedMinRange, excludedMaxRange)
        
        assertEquals(actual.toSet().size, actual.size, "Duplicate chunks")
        assertEquals(expected, actual.toSet(), "Incorrect chunks")
    }
    
    private fun chunksInRange(center: ChunkSec, range: Int): Set<ChunkSec> =
        buildSet { forEachChunkSecInRange(center, range, ::add) }

    private fun chunksInDonut(center: ChunkSec, minRange: Int, maxRange: Int): Set<ChunkSec> =
        buildSet { forEachChunkSecInDonut(center, minRange, maxRange, ::add) }
    
    companion object {
        
        @JvmStatic
        fun rangeDifferenceCases(): List<Array<Any>> = buildList {
            add(arrayOf(ChunkSec(0, 0, 0), 4, ChunkSec(0, 0, 0), 4))
            add(arrayOf(ChunkSec(0, 0, 0), 6, ChunkSec(0, 0, 0), 4))
            add(arrayOf(ChunkSec(0, 0, 0), 2, ChunkSec(0, 0, 0), 4))
            add(arrayOf(ChunkSec(0, 0, 0), 4, ChunkSec(1, 0, 0), 4))
            add(arrayOf(ChunkSec(0, 0, 0), 4, ChunkSec(1, 1, 1), 4))
            add(arrayOf(ChunkSec(0, 0, 0), 4, ChunkSec(20, 20, 20), 2))
            
            val random = Random(42)
            repeat(500) {
                add(
                    arrayOf(
                        ChunkSec(random.nextInt(-20, 21), random.nextInt(-20, 21), random.nextInt(-20, 21)),
                        random.nextInt(0, 6),
                        ChunkSec(random.nextInt(-20, 21), random.nextInt(-20, 21), random.nextInt(-20, 21)),
                        random.nextInt(0, 6)
                    )
                )
            }
        }

        @JvmStatic
        fun donutDifferenceCases(): List<Array<Any>> = buildList {
            add(arrayOf(ChunkSec(0, 0, 0), -1, 2, ChunkSec(1, 0, 0), -1, 2))
            add(arrayOf(ChunkSec(0, 0, 0), 2, 4, ChunkSec(1, 0, 0), 2, 4))
            add(arrayOf(ChunkSec(0, 0, 0), 4, 16, ChunkSec(1, 1, 1), 4, 8))
            add(arrayOf(ChunkSec(0, 0, 0), 4, 4, ChunkSec(0, 0, 0), -1, 4))
            add(arrayOf(ChunkSec(0, 0, 0), -1, 8, ChunkSec(0, 0, 0), 4, 16))
            
            val random = Random(43)
            repeat(500) {
                val minRange = random.nextInt(-1, 6)
                val excludedMinRange = random.nextInt(-1, 6)
                add(
                    arrayOf(
                        ChunkSec(random.nextInt(-10, 11), random.nextInt(-10, 11), random.nextInt(-10, 11)),
                        minRange,
                        random.nextInt(minRange + 1, 8),
                        ChunkSec(random.nextInt(-10, 11), random.nextInt(-10, 11), random.nextInt(-10, 11)),
                        excludedMinRange,
                        random.nextInt(excludedMinRange + 1, 8)
                    )
                )
            }
        }
        
    }
    
}
