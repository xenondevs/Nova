package xyz.xenondevs.nova.world.format.chunk.container

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SingleValueSectionDataContainerTest {
    
    @Test
    fun testRead() {
        val idResolver = MockIdResolver()
        val bin = byteWriter { writeVarInt(idResolver.toId("a")) }
        val container = SingleValueSectionDataContainer(idResolver, ByteReader.fromByteArray(bin))
        
        for (x in 0..<16) {
            for (y in 0..<16) {
                for (z in 0..<16) {
                    assertEquals("a", container[x, y, z])
                }
            }
        }
    }
    
    @Test
    fun testWrite() {
        val idResolver = MockIdResolver()
        val container = SingleValueSectionDataContainer(idResolver, "a")
        val bin = byteWriter { container.write(this) }
        
        val expectedBin = byteWriter { writeVarInt(idResolver.toId("a")) }
        assertContentEquals(expectedBin, bin)
    }
    
    @Test
    fun testNonEmptyBlockCount() {
        val idResolver = MockIdResolver()
        
        val containerA = SingleValueSectionDataContainer(idResolver, "a")
        assertEquals(SectionDataContainer.SECTION_SIZE, containerA.nonEmptyBlockCount)
        
        val containerNull = SingleValueSectionDataContainer(idResolver, null)
        assertEquals(0, containerNull.nonEmptyBlockCount)
    }
    
    @Test
    fun testGetSet() {
        val idResolver = MockIdResolver()
        val container = SingleValueSectionDataContainer(idResolver, "a")
        
        for (x in 0..<16) {
            for (y in 0..<16) {
                for (z in 0..<16) {
                    assertEquals("a", container[x, y, z])
                    assertThrows<Throwable> { container[x, y, z] = "b" }
                }
            }
        }
    }
    
}