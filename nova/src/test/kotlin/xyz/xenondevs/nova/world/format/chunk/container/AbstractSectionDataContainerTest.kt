package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.nova.byteWriter
import kotlin.test.assertEquals

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