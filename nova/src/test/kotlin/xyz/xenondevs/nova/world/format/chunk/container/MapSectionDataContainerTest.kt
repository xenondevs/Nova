package xyz.xenondevs.nova.world.format.chunk.container

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import xyz.xenondevs.nova.world.format.chunk.data.MappedCompactIntStorage
import xyz.xenondevs.nova.world.format.chunk.palette.HashPalette
import xyz.xenondevs.nova.world.format.chunk.palette.LinearPalette
import xyz.xenondevs.nova.world.format.chunk.palette.Palette
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapSectionDataContainerTest : AbstractSectionDataContainerTest() {
    
    @Test
    fun testRead() {
        val idResolver = MockIdResolver()
        val palette = LinearPalette(idResolver)
        
        palette.putValue("a")
        palette.putValue("b")
        palette.putValue("c")
        
        val binIn = byteWriter {
            Palette.write(palette, this)
            
            writeVarInt(3) // size
            
            // key-value pairs
            writeShort(0)
            writeByte(palette.getId("a").toByte())
            writeShort(1)
            writeByte(palette.getId("b").toByte())
            writeShort(2)
            writeByte(palette.getId("c").toByte())
        }
        
        val container = MapSectionDataContainer(idResolver, ByteReader.fromByteArray(binIn))
        
        assertEquals("a", container[0, 0, 0])
        assertEquals("b", container[0, 0, 1])
        assertEquals("c", container[0, 0, 2])
        assertNull(null, container[0, 0, 3])
        assertEquals(3, container.nonEmptyBlockCount)
    }
    
    @Test
    fun testWriteRead() {
        val idResolver = MockIdResolver()
        val container = MapSectionDataContainer(idResolver)
        container[0, 0, 0] = "a"
        container[0, 0, 1] = "b"
        container[0, 0, 2] = "c"
        
        val binOut = byteWriter { container.write(this) }
        val containerIn = MapSectionDataContainer(idResolver, ByteReader.fromByteArray(binOut))
        
        assertEquals("a", containerIn[0, 0, 0])
        assertEquals("b", containerIn[0, 0, 1])
        assertEquals("c", containerIn[0, 0, 2])
        assertNull(null, containerIn[0, 0, 3])
    }
    
    @Test
    fun testSwapPalette() {
        val idResolver = MockIdResolver()
        val container = MapSectionDataContainer(MockIdResolver())
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        
        for (i in 0..16)
            container[i / 16, 0, i % 16] = alphabet[i].toString()
        
        val binOut = byteWriter { container.write(this) }
        val expected = byteWriter {
            val palette = HashPalette(idResolver)
            for (i in 0..16)
                palette.putValue(alphabet[i].toString())
            
            val map = MappedCompactIntStorage.create(8)
            for (i in 0..16)
                map[i] = palette.getId(alphabet[i].toString())
            
            Palette.write(palette, this)
            map.write(this)
        }
        
        assertContentEquals(expected, binOut)
    }
    
    
    @Test
    fun testNonEmptyBlockCount() {
        testNonEmptyBlockCount(MapSectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testFill() {
        testFill(MapSectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testForEachNonEmpty() {
        testForEachNonEmpty(MapSectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testIsMonotone() {
        testIsMonotone(MapSectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testDataMigrateOnPaletteResize() {
        testDataMigrateOnPaletteResize(MapSectionDataContainer(MockIdResolver()))
    }
    
}