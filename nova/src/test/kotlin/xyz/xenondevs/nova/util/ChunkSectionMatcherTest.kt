package xyz.xenondevs.nova.util

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.GlobalPalette
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.SingleValuePalette
import net.minecraft.world.level.chunk.Strategy
import net.minecraft.server.Bootstrap
import net.minecraft.SharedConstants
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChunkSectionMatcherTest {
    
    @Test
    fun testSingleValuePalette() {
        val container = createContainer()
        assertIs<SingleValuePalette<*>>(container.data.palette())
        
        assertSliceMatches(container, setOf(Blocks.AIR.defaultBlockState()))
        assertSliceMatches(container, emptySet())
    }
    
    @Test
    fun testLinearPalette() {
        val container = createContainer()
        container[3, 4, 5] = Blocks.STONE.defaultBlockState()
        container[9, 8, 7] = Blocks.DIRT.defaultBlockState()
        assertIs<LinearPalette<*>>(container.data.palette())
        
        assertSliceMatches(container, setOf(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState()))
    }
    
    @Test
    fun testHashMapPalette() {
        val container = createContainer()
        val states = distinctStates(32)
        setStates(container, states)
        assertIs<HashMapPalette<*>>(container.data.palette())
        
        assertSliceMatches(container, states.filterIndexedTo(HashSet()) { index, _ -> index % 3 == 0 })
    }
    
    @Test
    fun testGlobalPalette() {
        val container = createContainer()
        val states = distinctStates(300)
        setStates(container, states)
        assertIs<GlobalPalette<*>>(container.data.palette())
        
        assertSliceMatches(container, states.filterIndexedTo(HashSet()) { index, _ -> index % 7 == 0 })
    }
    
    @Test
    fun testCrossSectionVolume() {
        val originX = -5
        val originY = 13
        val originZ = 14
        val width = 25
        val height = 19
        val depth = 20
        val containers = HashMap<Triple<Int, Int, Int>, PalettedContainer<BlockState>>()
        
        fun matches(x: Int, y: Int, z: Int): Boolean = Math.floorMod(x * 31 + y * 17 + z, 5) < 2
        
        for (z in originZ..<originZ + depth) {
            for (y in originY..<originY + height) {
                for (x in originX..<originX + width) {
                    val sectionPos = Triple(x shr 4, y shr 4, z shr 4)
                    val container = containers.getOrPut(sectionPos, ::createContainer)
                    container[x and 15, y and 15, z and 15] = if (matches(x, y, z))
                        Blocks.STONE.defaultBlockState()
                    else Blocks.DIRT.defaultBlockState()
                }
            }
        }
        
        val result = BlockStateMatcher(Blocks.STONE.defaultBlockState().bukkitBlockData).match(
            originX, originY, originZ,
            width, height, depth
        ) { sectionX, sectionY, sectionZ ->
            containers[Triple(sectionX, sectionY, sectionZ)]?.data
        }
        
        for (z in 0..<depth) {
            for (y in 0..<height) {
                for (x in 0..<width) {
                    assertEquals(
                        matches(originX + x, originY + y, originZ + z),
                        result[x, y, z],
                        "Mismatch at local ($x, $y, $z)"
                    )
                }
            }
        }
        
        val boxes = listOf(
            intArrayOf(0, 0, 0, width, height, depth),
            intArrayOf(3, 1, 1, 18, 17, 17),
            intArrayOf(5, 3, 2, 7, 9, 11),
            intArrayOf(20, 14, 15, 5, 5, 5)
        )
        for (box in boxes) {
            val x = box[0]
            val y = box[1]
            val z = box[2]
            val boxWidth = box[3]
            val boxHeight = box[4]
            val boxDepth = box[5]
            var expected = 0
            for (localZ in z..<z + boxDepth) {
                for (localY in y..<y + boxHeight) {
                    for (localX in x..<x + boxWidth) {
                        if (matches(originX + localX, originY + localY, originZ + localZ))
                            expected++
                    }
                }
            }
            assertEquals(expected, result.count(x, y, z, boxWidth, boxHeight, boxDepth))
            assertEquals(expected == boxWidth * boxHeight * boxDepth, result.matchesAll(x, y, z, boxWidth, boxHeight, boxDepth))
        }
    }
    
    private fun assertSliceMatches(container: PalettedContainer<BlockState>, matches: Set<BlockState>) {
        val minX = 3
        val minY = 2
        val minZ = 1
        val maxX = 14
        val maxY = 11
        val maxZ = 13
        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ
        val destinationOffset = 5
        val destinationYStride = width + 3
        val destinationZStride = destinationYStride * (height + 2)
        val destination = IntArray(destinationOffset + destinationZStride * depth) { -1 }
        
        val blockData = matches.map { it.bukkitBlockData }
        val matcher = if (blockData.size == 1)
            BlockStateMatcher(blockData.single())
        else BlockStateMatcher(blockData)
        matcher.copyMatches(
            container.data,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            destination, destinationOffset, destinationYStride, destinationZStride
        )
        
        for (z in minZ..<maxZ) {
            for (y in minY..<maxY) {
                for (x in minX..<maxX) {
                    val destinationIndex = destinationOffset +
                        (x - minX) +
                        (y - minY) * destinationYStride +
                        (z - minZ) * destinationZStride
                    val expected = if (container[x, y, z] in matches) 1 else 0
                    assertEquals(expected, destination[destinationIndex], "Mismatch at ($x, $y, $z)")
                }
            }
        }
        
        assertEquals(-1, destination[0])
        assertEquals(-1, destination[destinationOffset + width])
    }
    
    private fun createContainer(): PalettedContainer<BlockState> = PalettedContainer(
        Blocks.AIR.defaultBlockState(),
        Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY),
        null
    )
    
    private fun distinctStates(count: Int): List<BlockState> {
        return (0..<Block.BLOCK_STATE_REGISTRY.size())
            .mapNotNull(Block.BLOCK_STATE_REGISTRY::byId)
            .take(count)
            .also { check(it.size == count) }
    }
    
    private fun setStates(container: PalettedContainer<BlockState>, states: List<BlockState>) {
        for (index in states.indices) {
            val x = index and 0xF
            val z = index shr 4 and 0xF
            val y = index shr 8 and 0xF
            container[x, y, z] = states[index]
        }
    }
    
    companion object {
        
        @BeforeAll
        @JvmStatic
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
        
    }
    
}
