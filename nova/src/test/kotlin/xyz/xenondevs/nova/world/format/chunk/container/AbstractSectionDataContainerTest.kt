package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.SectionMatchResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class AbstractSectionDataContainerTest {
    
    internal fun testNonEmptyBlockCount(container: SectionDataContainer<String>) {
        assertEquals(0, container.nonEmptyBlockCount)
        container[0, 0, 0] = "a"
        assertEquals(1, container.nonEmptyBlockCount)
        container[0, 0, 0] = "b"
        assertEquals(1, container.nonEmptyBlockCount)
        container[0, 0, 0] = null
        assertEquals(0, container.nonEmptyBlockCount)
    }
    
    internal fun testFill(container: SectionDataContainer<String>) {
        container.fill("a")
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in 0..15) {
                    assertEquals("a", container[x, y, z])
                }
            }
        }
    }
    
    internal fun testForEachNonEmpty(container: SectionDataContainer<String>) {
        container[1, 1, 1] = "b"
        
        container.forEachNonEmpty { x, y, z, value ->
            assertEquals(1, x)
            assertEquals(1, y)
            assertEquals(1, z)
            assertEquals("b", value)
        }
    }
    
    internal fun testIsMonotone(container: SectionDataContainer<String>) {
        container.fill(null)
        assertEquals(true, container.isMonotone())
        
        container[0, 0, 0] = "a"
        assertEquals(false, container.isMonotone())
        
        container.fill("a")
        assertEquals(true, container.isMonotone())
        
        container[0, 0, 0] = "b"
        assertEquals(false, container.isMonotone())
    }
    
    internal fun testMatchEmptyContainer(container: SectionDataContainer<String>) {
        // empty container, non-empty match set -> NONE
        val result = container.match(setOf("a"))
        assertNoneTrue(result)
    }
    
    internal fun testMatchEmptySet(container: SectionDataContainer<String>) {
        container[0, 0, 0] = "a"
        container[1, 2, 3] = "b"
        // populated container, empty match set -> NONE
        val result = container.match(emptySet())
        assertNoneTrue(result)
    }
    
    internal fun testMatchValuesNotInPalette(container: SectionDataContainer<String>) {
        container[0, 0, 0] = "a"
        // values that have never been added to the palette -> NONE
        val result = container.match(setOf("z", "y"))
        assertNoneTrue(result)
    }
    
    internal fun testMatchSingleValue(container: SectionDataContainer<String>) {
        container[0, 0, 0] = "a"
        container[1, 2, 3] = "b"
        container[4, 5, 6] = "a"
        container[15, 15, 15] = "c"
        
        val result = container.match(setOf("a"))
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in 0..15) {
                    val expected = (x == 0 && y == 0 && z == 0) || (x == 4 && y == 5 && z == 6)
                    assertEquals(expected, result[x, y, z], "($x,$y,$z)")
                }
            }
        }
    }
    
    internal fun testMatchMultipleValues(container: SectionDataContainer<String>) {
        container[0, 0, 0] = "a"
        container[1, 2, 3] = "b"
        container[4, 5, 6] = "c"
        container[7, 8, 9] = "d"
        
        val result = container.match(setOf("a", "b"))
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in 0..15) {
                    val expected = (x == 0 && y == 0 && z == 0) || (x == 1 && y == 2 && z == 3)
                    assertEquals(expected, result[x, y, z], "($x,$y,$z)")
                }
            }
        }
    }
    
    internal fun testMatchMultipleValuesPartiallyInPalette(container: SectionDataContainer<String>) {
        container[0, 0, 0] = "a"
        container[1, 2, 3] = "b"
        
        // "z" and "y" never added to palette, "a" is — result should mark only "a"
        val result = container.match(setOf("a", "z", "y"))
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in 0..15) {
                    val expected = (x == 0 && y == 0 && z == 0)
                    assertEquals(expected, result[x, y, z], "($x,$y,$z)")
                }
            }
        }
    }
    
    private fun assertNoneTrue(result: SectionMatchResult) {
        for (i in 0..<4096) {
            assertFalse(result[i], "expected false at $i")
        }
    }
    
    internal fun testDataMigrateOnPaletteResize(container: SectionDataContainer<String>) {
        container[0, 0, 1] = "a"
        container[0, 0, 2] = "b"
        container[0, 0, 3] = "b"
        container[0, 0, 4] = "c"
        
        container[0, 0, 1] = null
        
        // write to cause palette remake
        byteWriter { container.write(this) }
        
        assertEquals("b", container[0, 0, 2])
        assertEquals("b", container[0, 0, 3])
        assertEquals("c", container[0, 0, 4])
    }
    
}